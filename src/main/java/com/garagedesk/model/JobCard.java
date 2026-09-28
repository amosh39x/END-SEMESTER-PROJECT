package com.garagedesk.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
public class JobCard {
    public enum Status { WAITING, SCHEDULED, IN_PROGRESS, COMPLETED }
    public enum Priority { URGENT, HIGH, NORMAL }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @NotBlank public String vehicleNumber;
    public String vehicleModel;
    @NotBlank public String customerName;
    public String customerPhone;
    @NotBlank public String issue;
    @Enumerated(EnumType.STRING) public Status status = Status.WAITING;
    @Enumerated(EnumType.STRING) public Priority priority = Priority.NORMAL;
    public int progress;
    @ManyToOne public Bay bay;
    @ManyToOne public Mechanic mechanic;
    public LocalDateTime createdAt, scheduledAt, completedAt;
}
