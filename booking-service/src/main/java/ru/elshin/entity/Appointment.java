package ru.elshin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId; // Храним только ID, без связей JPA

    @Column(name = "master_id", nullable = false)
    private Long masterId; // Храним только ID, без связей JPA

    @Column(nullable = false)
    private String serviceName; // Название услуги

    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;
}
