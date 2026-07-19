package ec.edu.ups.icc.academiceventsapi.report.dto;

public record SystemStatsResponse(
        long totalUsers,
        long totalCategories,
        long totalEvents,
        long draftEvents,
        long publishedEvents,
        long finishedEvents,
        long cancelledEvents,
        long totalRegistrations,
        long pendingRegistrations,
        long confirmedRegistrations,
        long rejectedRegistrations,
        long cancelledRegistrations
) {
}
