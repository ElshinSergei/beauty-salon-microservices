package ru.elshin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentEvent {
    private Long appointmentId;
    private Long clientId;
    private String serviceName;
    private String appointmentTime;
}
