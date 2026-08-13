package com.wasbyte.headcount.headcount.entity;

import com.wasbyte.headcount.user.entity.User;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "headcount_participants",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_headcount_participants_event_employee",
                columnNames = {"event_id", "employee_id"}
        )
)
public class HeadcountParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private HeadcountEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Column(name = "employee_name_snapshot", nullable = false, length = 201)
    private String employeeNameSnapshot;

    @Column(name = "resource_number_snapshot", nullable = false, length = 100)
    private String resourceNumberSnapshot;

    @Column(name = "organization_path_snapshot", nullable = false, length = 1000)
    private String organizationPathSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HeadcountParticipantStatus status;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Column(name = "confirmation_source", length = 100)
    private String confirmationSource;

    @Column(name = "help_message", length = 1000)
    private String helpMessage;

    @Column(name = "help_requested_at")
    private LocalDateTime helpRequestedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public HeadcountParticipant() {
    }

    public Long getId() { return id; }
    public HeadcountEvent getEvent() { return event; }
    public void setEvent(HeadcountEvent event) { this.event = event; }
    public User getEmployee() { return employee; }
    public void setEmployee(User employee) { this.employee = employee; }
    public String getEmployeeNameSnapshot() { return employeeNameSnapshot; }
    public void setEmployeeNameSnapshot(String value) { this.employeeNameSnapshot = value; }
    public String getResourceNumberSnapshot() { return resourceNumberSnapshot; }
    public void setResourceNumberSnapshot(String value) { this.resourceNumberSnapshot = value; }
    public String getOrganizationPathSnapshot() { return organizationPathSnapshot; }
    public void setOrganizationPathSnapshot(String value) { this.organizationPathSnapshot = value; }
    public HeadcountParticipantStatus getStatus() { return status; }
    public void setStatus(HeadcountParticipantStatus status) { this.status = status; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public User getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(User confirmedBy) { this.confirmedBy = confirmedBy; }
    public String getConfirmationSource() { return confirmationSource; }
    public void setConfirmationSource(String confirmationSource) { this.confirmationSource = confirmationSource; }
    public String getHelpMessage() { return helpMessage; }
    public void setHelpMessage(String helpMessage) { this.helpMessage = helpMessage; }
    public LocalDateTime getHelpRequestedAt() { return helpRequestedAt; }
    public void setHelpRequestedAt(LocalDateTime value) { this.helpRequestedAt = value; }
    public long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
