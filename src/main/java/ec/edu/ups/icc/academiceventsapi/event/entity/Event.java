package ec.edu.ups.icc.academiceventsapi.event.entity;

import ec.edu.ups.icc.academiceventsapi.category.entity.Category;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "modality", nullable = false, length = 20)
    private EventModality modality;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "virtual_url", length = 500)
    private String virtualUrl;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "available_capacity", nullable = false)
    private Integer availableCapacity;

    @Column(name = "registration_start_at", nullable = false)
    private Instant registrationStartAt;

    @Column(name = "registration_end_at", nullable = false)
    private Instant registrationEndAt;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "event")
    private List<Session> sessions = new ArrayList<>();

    protected Event() {
    }

    public Event(String title, String description, EventModality modality, Integer capacity,
                 Instant registrationStartAt, Instant registrationEndAt,
                 Instant startAt, Instant endAt, User organizer, Category category) {
        this.title = title;
        this.description = description;
        this.modality = modality;
        this.capacity = capacity;
        this.availableCapacity = capacity;
        this.registrationStartAt = registrationStartAt;
        this.registrationEndAt = registrationEndAt;
        this.startAt = startAt;
        this.endAt = endAt;
        this.organizer = organizer;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EventModality getModality() {
        return modality;
    }

    public void setModality(EventModality modality) {
        this.modality = modality;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getVirtualUrl() {
        return virtualUrl;
    }

    public void setVirtualUrl(String virtualUrl) {
        this.virtualUrl = virtualUrl;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(Integer availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public Instant getRegistrationStartAt() {
        return registrationStartAt;
    }

    public void setRegistrationStartAt(Instant registrationStartAt) {
        this.registrationStartAt = registrationStartAt;
    }

    public Instant getRegistrationEndAt() {
        return registrationEndAt;
    }

    public void setRegistrationEndAt(Instant registrationEndAt) {
        this.registrationEndAt = registrationEndAt;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public User getOrganizer() {
        return organizer;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Session> getSessions() {
        return sessions;
    }
}
