package ru.elshin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentStatusChangedEvent {
    private Long appointmentId;
    private Long clientId;
    private Long masterId;
    private String serviceName;
    private LocalDateTime appointmentTime;
    private String previousStatus;
    private String newStatus;
}
