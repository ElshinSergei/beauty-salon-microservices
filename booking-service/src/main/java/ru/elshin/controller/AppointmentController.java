package ru.elshin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.elshin.entity.Appointment;
import ru.elshin.service.AppointmentService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<Appointment> createAppointment(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Appointment appointment) {

        // Гарантируем, что запись создаётся именно для авторизованного пользователя
        appointment.setClientId(userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(appointment));
    }

    @GetMapping
    public ResponseEntity<List<Appointment>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    // Получение записей ТЕКУЩЕГО авторизованного клиента
    @GetMapping("/my")
    public ResponseEntity<List<Appointment>> getMyAppointments(@RequestHeader("X-User-Id") Long userId) {
        List<Appointment> myAppointments = appointmentService.getAppointmentsByUserId(userId);
        return ResponseEntity.ok(myAppointments);
    }

    @GetMapping("/master/{masterId}")
    public ResponseEntity<List<Appointment>> getMasterAppointments(
            @PathVariable Long masterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (date != null) {
            // Если дата передана, отдаем расписание на день
            List<Appointment> dailyAppointments = appointmentService.getAppointmentsByMasterAndDate(masterId, date);
            return ResponseEntity.ok(dailyAppointments);
        }
        // Если даты нет, отдаем вообще все записи мастера
        List<Appointment> allAppointments = appointmentService.getAppointmentsByMaster(masterId);
        return ResponseEntity.ok(allAppointments);
    }

    // Подтверждение записи мастером
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<Appointment> confirmAppointment(@PathVariable Long id) {
        Appointment confirmed = appointmentService.confirmAppointment(id);
        return ResponseEntity.ok(confirmed);
    }

    // Отмена записи
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Appointment> cancelAppointment(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        Appointment cancelled = appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(cancelled);
    }
}
