package ec.edu.ups.icc.academiceventsapi.report.service;

import ec.edu.ups.icc.academiceventsapi.category.repository.CategoryRepository;
import ec.edu.ups.icc.academiceventsapi.common.exception.BusinessRuleViolationException;
import ec.edu.ups.icc.academiceventsapi.common.exception.ResourceNotFoundException;
import ec.edu.ups.icc.academiceventsapi.event.entity.Event;
import ec.edu.ups.icc.academiceventsapi.event.entity.EventStatus;
import ec.edu.ups.icc.academiceventsapi.event.repository.EventRepository;
import ec.edu.ups.icc.academiceventsapi.ratelimit.RateLimitExceededException;
import ec.edu.ups.icc.academiceventsapi.ratelimit.RateLimitResult;
import ec.edu.ups.icc.academiceventsapi.ratelimit.RateLimiterService;
import ec.edu.ups.icc.academiceventsapi.registration.entity.Registration;
import ec.edu.ups.icc.academiceventsapi.registration.entity.RegistrationStatus;
import ec.edu.ups.icc.academiceventsapi.registration.repository.RegistrationRepository;
import ec.edu.ups.icc.academiceventsapi.registration.repository.RegistrationSpecifications;
import ec.edu.ups.icc.academiceventsapi.report.dto.EventStatsResponse;
import ec.edu.ups.icc.academiceventsapi.report.dto.SystemStatsResponse;
import ec.edu.ups.icc.academiceventsapi.user.entity.RoleName;
import ec.edu.ups.icc.academiceventsapi.user.entity.User;
import ec.edu.ups.icc.academiceventsapi.user.repository.UserRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RateLimiterService rateLimiterService;

    public ReportServiceImpl(EventRepository eventRepository, RegistrationRepository registrationRepository,
                              UserRepository userRepository, CategoryRepository categoryRepository,
                              RateLimiterService rateLimiterService) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateRegistrationsPdf(Long eventId, Instant from, Instant to, User actor) {
        checkReportRateLimit(actor);
        Event event = findEventOrThrow(eventId);
        assertEventOwnerOrAdmin(event, actor);
        List<Registration> registrations = findRegistrations(eventId, from, to);
        return renderRegistrationsPdf(event, registrations);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateRegistrationsExcel(Long eventId, Instant from, Instant to, User actor) {
        checkReportRateLimit(actor);
        Event event = findEventOrThrow(eventId);
        assertEventOwnerOrAdmin(event, actor);
        List<Registration> registrations = findRegistrations(eventId, from, to);
        return renderRegistrationsExcel(event, registrations);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateCertificate(Long registrationId, User participant) {
        checkReportRateLimit(participant);

        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la inscripción solicitada."));

        if (!registration.getParticipant().getId().equals(participant.getId())) {
            throw new AccessDeniedException("No puede descargar el comprobante de otro participante.");
        }
        if (registration.getStatus() != RegistrationStatus.CONFIRMED) {
            throw new BusinessRuleViolationException("REGISTRATION_NOT_CONFIRMED",
                    "Solo se puede generar el comprobante de una inscripción confirmada.");
        }

        return renderCertificatePdf(registration);
    }

    @Override
    @Transactional(readOnly = true)
    public SystemStatsResponse getSystemStats() {
        return new SystemStatsResponse(
                userRepository.count(),
                categoryRepository.count(),
                eventRepository.countByDeletedFalse(),
                eventRepository.countByStatusAndDeletedFalse(EventStatus.DRAFT),
                eventRepository.countByStatusAndDeletedFalse(EventStatus.PUBLISHED),
                eventRepository.countByStatusAndDeletedFalse(EventStatus.FINISHED),
                eventRepository.countByStatusAndDeletedFalse(EventStatus.CANCELLED),
                registrationRepository.count(),
                registrationRepository.countByStatus(RegistrationStatus.PENDING),
                registrationRepository.countByStatus(RegistrationStatus.CONFIRMED),
                registrationRepository.countByStatus(RegistrationStatus.REJECTED),
                registrationRepository.countByStatus(RegistrationStatus.CANCELLED)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public EventStatsResponse getEventStats(Long eventId, User actor) {
        Event event = findEventOrThrow(eventId);
        assertEventOwnerOrAdmin(event, actor);

        long pending = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.PENDING);
        long confirmed = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED);
        long rejected = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.REJECTED);
        long cancelled = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CANCELLED);
        double occupancyRate = event.getCapacity() == 0 ? 0.0 : (double) confirmed / event.getCapacity();

        return new EventStatsResponse(event.getId(), event.getTitle(), event.getCapacity(),
                event.getAvailableCapacity(), pending, confirmed, rejected, cancelled, occupancyRate);
    }

    private List<Registration> findRegistrations(Long eventId, Instant from, Instant to) {
        Specification<Registration> spec = Specification.allOf(
                RegistrationSpecifications.byEvent(eventId),
                RegistrationSpecifications.registeredFrom(from),
                RegistrationSpecifications.registeredTo(to)
        );
        return registrationRepository.findAll(spec);
    }

    private byte[] renderRegistrationsPdf(Event event, List<Registration> registrations) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font subtitleFont = new Font(Font.HELVETICA, 12, Font.NORMAL);
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font cellFont = new Font(Font.HELVETICA, 9, Font.NORMAL);

            document.add(new Paragraph("Listado de inscritos", titleFont));
            document.add(new Paragraph(event.getTitle(), subtitleFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 3, 2, 3});
            table.addCell(new Phrase("Participante", headerFont));
            table.addCell(new Phrase("Correo", headerFont));
            table.addCell(new Phrase("Estado", headerFont));
            table.addCell(new Phrase("Fecha de inscripción", headerFont));

            for (Registration registration : registrations) {
                table.addCell(new Phrase(participantName(registration), cellFont));
                table.addCell(new Phrase(registration.getParticipant().getEmail(), cellFont));
                table.addCell(new Phrase(registration.getStatus().name(), cellFont));
                table.addCell(new Phrase(DATE_FORMATTER.format(registration.getRegisteredAt()), cellFont));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el reporte PDF.", e);
        }
    }

    private byte[] renderCertificatePdf(Registration registration) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 12, Font.NORMAL);

            document.add(new Paragraph("Comprobante de inscripción", titleFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Evento: " + registration.getEvent().getTitle(), normalFont));
            document.add(new Paragraph("Participante: " + participantName(registration), normalFont));
            document.add(new Paragraph("Correo: " + registration.getParticipant().getEmail(), normalFont));
            document.add(new Paragraph("Código de inscripción: " + registration.getRegistrationCode(), normalFont));
            document.add(new Paragraph("Estado: " + registration.getStatus(), normalFont));
            document.add(new Paragraph("Fecha de confirmación: "
                    + DATE_FORMATTER.format(registration.getConfirmedAt()), normalFont));

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el certificado.", e);
        }
    }

    private byte[] renderRegistrationsExcel(Event event, List<Registration> registrations) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inscripciones");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Participante");
            header.createCell(1).setCellValue("Correo");
            header.createCell(2).setCellValue("Estado");
            header.createCell(3).setCellValue("Fecha de inscripción");

            int rowIndex = 1;
            for (Registration registration : registrations) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(participantName(registration));
                row.createCell(1).setCellValue(registration.getParticipant().getEmail());
                row.createCell(2).setCellValue(registration.getStatus().name());
                row.createCell(3).setCellValue(DATE_FORMATTER.format(registration.getRegisteredAt()));
            }

            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el reporte Excel.", e);
        }
    }

    private String participantName(Registration registration) {
        return registration.getParticipant().getFirstName() + " " + registration.getParticipant().getLastName();
    }

    private Event findEventOrThrow(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el evento solicitado."));
        if (event.isDeleted()) {
            throw new ResourceNotFoundException("No se encontró el evento solicitado.");
        }
        return event;
    }

    private void assertEventOwnerOrAdmin(Event event, User actor) {
        if (!actor.hasRole(RoleName.ADMIN) && !event.getOrganizer().getId().equals(actor.getId())) {
            throw new AccessDeniedException("No puede generar reportes de eventos de otro organizador.");
        }
    }

    private void checkReportRateLimit(User actor) {
        RateLimitResult result = rateLimiterService.tryConsume("rate-limit:reports:" + actor.getId(), 5,
                Duration.ofMinutes(1));
        if (!result.allowed()) {
            throw new RateLimitExceededException(
                    "Demasiadas solicitudes de generación de reportes. Intente más tarde.",
                    result.retryAfterSeconds());
        }
    }
}
