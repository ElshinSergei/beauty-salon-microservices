package ru.elshin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Appointments", description = "API для управления записями") // Аннотация для контроллера
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @Operation(summary = "Создать новую запись", description = "Создает запись для авторизованного клиента")
    @ApiResponse(responseCode = "201", description = "Запись успешно создана")
    public ResponseEntity<Appointment> createAppointment(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Appointment appointment) {

        // Гарантируем, что запись создаётся именно для авторизованного пользователя
        appointment.setClientId(userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(appointment));
    }

    @GetMapping
    @Operation(summary = "Получить все записи", description = "Возвращает список всех записей (административная функция)")
    @ApiResponse(responseCode = "200", description = "Список записей получен")

    public ResponseEntity<List<Appointment>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @GetMapping("/my")
    @Operation(summary = "Мои записи", description = "Получение записей текущего авторизованного клиента")
    @ApiResponse(responseCode = "200", description = "Список записей клиента")
    public ResponseEntity<List<Appointment>> getMyAppointments(@RequestHeader("X-User-Id") Long userId) {
        List<Appointment> myAppointments = appointmentService.getAppointmentsByUserId(userId);
        return ResponseEntity.ok(myAppointments);
    }

    @GetMapping("/master/{masterId}")
    @Operation(summary = "Записи мастера", description = "Получение всех записей мастера или записей на конкретную дату")
    @ApiResponse(responseCode = "200", description = "Список записей мастера")
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

    @PatchMapping("/{id}/confirm")
    @Operation(summary = "Подтвердить запись", description = "Подтверждение записи мастером")
    @ApiResponse(responseCode = "200", description = "Запись подтверждена")
    public ResponseEntity<Appointment> confirmAppointment(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        Appointment confirmed = appointmentService.confirmAppointment(id, userId);
        return ResponseEntity.ok(confirmed);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Отменить запись", description = "Отмена записи (клиентом или мастером)")
    @ApiResponse(responseCode = "200", description = "Запись отменена")
    public ResponseEntity<Appointment> cancelAppointment(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        Appointment cancelled = appointmentService.cancelAppointment(id, userId);
        return ResponseEntity.ok(cancelled);
    }
}
