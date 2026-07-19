package ec.edu.ups.icc.academiceventsapi.registration.controller;

import ec.edu.ups.icc.academiceventsapi.auth.security.CustomUserDetails;
import ec.edu.ups.icc.academiceventsapi.registration.dto.RegistrationResponse;
import ec.edu.ups.icc.academiceventsapi.registration.dto.RegistrationStatusRequest;
import ec.edu.ups.icc.academiceventsapi.registration.entity.RegistrationStatus;
import ec.edu.ups.icc.academiceventsapi.registration.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/{eventId}/registrations")
public class EventRegistrationController {

    private final RegistrationService registrationService;

    public EventRegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public Page<RegistrationResponse> list(@PathVariable Long eventId,
                                            @RequestParam(required = false) RegistrationStatus status,
                                            @PageableDefault(size = 20, sort = "registeredAt") Pageable pageable,
                                            @AuthenticationPrincipal CustomUserDetails principal) {
        return registrationService.listByEvent(eventId, status, principal.getUser(), pageable);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public RegistrationResponse changeStatus(@PathVariable Long eventId, @PathVariable Long id,
                                              @Valid @RequestBody RegistrationStatusRequest request,
                                              @AuthenticationPrincipal CustomUserDetails principal) {
        return registrationService.changeStatus(eventId, id, request.status(), principal.getUser());
    }
}
