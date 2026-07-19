package ec.edu.ups.icc.academiceventsapi.report.dto;

public record EventStatsResponse(
        Long eventId,
        String eventTitle,
        Integer capacity,
        Integer availableCapacity,
        long pendingRegistrations,
        long confirmedRegistrations,
        long rejectedRegistrations,
        long cancelledRegistrations,
        double occupancyRate
) {
}
