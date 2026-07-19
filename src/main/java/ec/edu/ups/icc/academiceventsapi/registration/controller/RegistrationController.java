package ec.edu.ups.icc.academiceventsapi.registration.controller;

import ec.edu.ups.icc.academiceventsapi.auth.security.CustomUserDetails;
import ec.edu.ups.icc.academiceventsapi.registration.dto.RegistrationRequest;
import ec.edu.ups.icc.academiceventsapi.registration.dto.RegistrationResponse;
import ec.edu.ups.icc.academiceventsapi.registration.entity.RegistrationStatus;
import ec.edu.ups.icc.academiceventsapi.registration.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PARTICIPANT')")
    public RegistrationResponse register(@Valid @RequestBody RegistrationRequest request,
                                          @AuthenticationPrincipal CustomUserDetails principal) {
        return registrationService.register(request.eventId(), principal.getUser());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('PARTICIPANT')")
    public Page<RegistrationResponse> listMine(@RequestParam(required = false) RegistrationStatus status,
                                                @PageableDefault(size = 20, sort = "registeredAt") Pageable pageable,
                                                @AuthenticationPrincipal CustomUserDetails principal) {
        return registrationService.listMine(principal.getUser(), status, pageable);
    }

    @GetMapping("/{id}")
    public RegistrationResponse getById(@PathVariable Long id,
                                         @AuthenticationPrincipal CustomUserDetails principal) {
        return registrationService.getById(id, principal.getUser());
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PARTICIPANT')")
    public RegistrationResponse cancel(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        return registrationService.cancel(id, principal.getUser());
    }
}
