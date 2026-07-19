package ec.edu.ups.icc.academiceventsapi.event.controller;

import ec.edu.ups.icc.academiceventsapi.auth.security.CustomUserDetails;
import ec.edu.ups.icc.academiceventsapi.event.dto.SessionRequest;
import ec.edu.ups.icc.academiceventsapi.event.dto.SessionResponse;
import ec.edu.ups.icc.academiceventsapi.event.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public List<SessionResponse> list(@PathVariable Long eventId,
                                       @AuthenticationPrincipal CustomUserDetails principal) {
        return sessionService.listByEvent(eventId, principal == null ? null : principal.getUser());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public SessionResponse create(@PathVariable Long eventId, @Valid @RequestBody SessionRequest request,
                                   @AuthenticationPrincipal CustomUserDetails principal) {
        return sessionService.create(eventId, request, principal.getUser());
    }

    @PutMapping("/{sessionId}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public SessionResponse update(@PathVariable Long eventId, @PathVariable Long sessionId,
                                   @Valid @RequestBody SessionRequest request,
                                   @AuthenticationPrincipal CustomUserDetails principal) {
        return sessionService.update(eventId, sessionId, request, principal.getUser());
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public void delete(@PathVariable Long eventId, @PathVariable Long sessionId,
                        @AuthenticationPrincipal CustomUserDetails principal) {
        sessionService.delete(eventId, sessionId, principal.getUser());
    }
}
