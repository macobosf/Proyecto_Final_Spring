package ec.edu.ups.icc.academiceventsapi.event.controller;

import ec.edu.ups.icc.academiceventsapi.auth.security.CustomUserDetails;
import ec.edu.ups.icc.academiceventsapi.event.dto.EventRequest;
import ec.edu.ups.icc.academiceventsapi.event.dto.EventResponse;
import ec.edu.ups.icc.academiceventsapi.event.dto.EventStatusRequest;
import ec.edu.ups.icc.academiceventsapi.event.entity.EventModality;
import ec.edu.ups.icc.academiceventsapi.event.entity.EventStatus;
import ec.edu.ups.icc.academiceventsapi.event.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public Page<EventResponse> listPublic(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) EventModality modality,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTo,
            @PageableDefault(size = 20, sort = "startAt") Pageable pageable) {
        return eventService.listPublic(categoryId, modality, search, startFrom, startTo, pageable);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('ORGANIZER')")
    public Page<EventResponse> listMine(@RequestParam(required = false) EventStatus status,
                                         @PageableDefault(size = 20, sort = "startAt") Pageable pageable,
                                         @AuthenticationPrincipal CustomUserDetails principal) {
        return eventService.listMine(principal.getUser(), status, pageable);
    }

    @GetMapping("/{id}")
    public EventResponse getById(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        return eventService.getById(id, principal == null ? null : principal.getUser());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ORGANIZER')")
    public EventResponse create(@Valid @RequestBody EventRequest request,
                                 @AuthenticationPrincipal CustomUserDetails principal) {
        return eventService.create(request, principal.getUser());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request,
                                 @AuthenticationPrincipal CustomUserDetails principal) {
        return eventService.update(id, request, principal.getUser());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public EventResponse changeStatus(@PathVariable Long id, @Valid @RequestBody EventStatusRequest request,
                                       @AuthenticationPrincipal CustomUserDetails principal) {
        return eventService.changeStatus(id, request.status(), principal.getUser());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        eventService.delete(id, principal.getUser());
    }
}
