package ec.edu.ups.icc.academiceventsapi.report.service;

import ec.edu.ups.icc.academiceventsapi.report.dto.EventStatsResponse;
import ec.edu.ups.icc.academiceventsapi.report.dto.SystemStatsResponse;
import ec.edu.ups.icc.academiceventsapi.user.entity.User;

import java.time.Instant;

public interface ReportService {

    byte[] generateRegistrationsPdf(Long eventId, Instant from, Instant to, User actor);

    byte[] generateRegistrationsExcel(Long eventId, Instant from, Instant to, User actor);

    byte[] generateCertificate(Long registrationId, User participant);

    SystemStatsResponse getSystemStats();

    EventStatsResponse getEventStats(Long eventId, User actor);
}
