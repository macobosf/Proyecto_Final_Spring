package ec.edu.ups.icc.academiceventsapi.report.controller;

import ec.edu.ups.icc.academiceventsapi.auth.security.CustomUserDetails;
import ec.edu.ups.icc.academiceventsapi.report.dto.EventStatsResponse;
import ec.edu.ups.icc.academiceventsapi.report.dto.SystemStatsResponse;
import ec.edu.ups.icc.academiceventsapi.report.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/events/{eventId}/registrations.pdf")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<byte[]> registrationsPdf(
            @PathVariable Long eventId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @AuthenticationPrincipal CustomUserDetails principal) {
        byte[] content = reportService.generateRegistrationsPdf(eventId, from, to, principal.getUser());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"registrations-event-" + eventId + ".pdf\"")
                .body(content);
    }

    @GetMapping("/events/{eventId}/registrations.xlsx")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<byte[]> registrationsExcel(
            @PathVariable Long eventId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @AuthenticationPrincipal CustomUserDetails principal) {
        byte[] content = reportService.generateRegistrationsExcel(eventId, from, to, principal.getUser());
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"registrations-event-" + eventId + ".xlsx\"")
                .body(content);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public SystemStatsResponse systemStats() {
        return reportService.getSystemStats();
    }

    @GetMapping("/events/{eventId}/stats")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public EventStatsResponse eventStats(@PathVariable Long eventId,
                                          @AuthenticationPrincipal CustomUserDetails principal) {
        return reportService.getEventStats(eventId, principal.getUser());
    }
}
