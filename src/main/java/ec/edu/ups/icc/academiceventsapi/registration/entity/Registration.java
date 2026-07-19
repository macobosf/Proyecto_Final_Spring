package ec.edu.ups.icc.academiceventsapi.registration.entity;

import ec.edu.ups.icc.academiceventsapi.event.entity.Event;
import ec.edu.ups.icc.academiceventsapi.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registrations")
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_code", nullable = false, updatable = false, unique = true)
    private UUID registrationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private User participant;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    @Column(name = "status_updated_at", nullable = false)
    private Instant statusUpdatedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Registration() {
    }

    public Registration(Event event, User participant) {
        this.registrationCode = UUID.randomUUID();
        this.event = event;
        this.participant = participant;
        this.statusUpdatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getRegistrationCode() {
        return registrationCode;
    }

    public Event getEvent() {
        return event;
    }

    public User getParticipant() {
        return participant;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Instant getStatusUpdatedAt() {
        return statusUpdatedAt;
    }

    public void setStatusUpdatedAt(Instant statusUpdatedAt) {
        this.statusUpdatedAt = statusUpdatedAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Long getVersion() {
        return version;
    }
}
