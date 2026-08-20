package ru.elshin.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.elshin.client.UserClient;
import ru.elshin.dto.UserDto;
import ru.elshin.entity.Appointment;
import ru.elshin.entity.AppointmentStatus;
import ru.elshin.repository.AppointmentRepository;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class AppointmentControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @MockBean
    private UserClient userClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        UserDto mockUser = new UserDto(1L, "Client", "client@test.com", "CLIENT");
        UserDto mockMaster = new UserDto(2L, "Master", "master@test.com", "MASTER");
        Mockito.when(userClient.getUserById(1L)).thenReturn(mockUser);
        Mockito.when(userClient.getUserById(2L)).thenReturn(mockMaster);
    }

    @Test
    void createAppointment_ShouldReturnCreated() throws Exception {
        String json = "{\"masterId\": 2, \"serviceName\": \"Haircut\", \"appointmentTime\": \"2026-08-25T10:00:00\"}";

        mockMvc.perform(post("/api/v1/appointments")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.masterId").value(2))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createAppointment_ShouldReturnConflict_WhenMasterIsBusy() throws Exception {
        // Создаем первую запись
        Appointment appointment = Appointment.builder()
                .clientId(1L).masterId(2L).serviceName("Haircut")
                .appointmentTime(LocalDateTime.of(2026, 8, 25, 10, 0))
                .status(AppointmentStatus.PENDING).build();
        appointmentRepository.save(appointment);

        String json = "{\"masterId\": 2, \"serviceName\": \"Haircut\", \"appointmentTime\": \"2026-08-25T10:00:00\"}";

        mockMvc.perform(post("/api/v1/appointments")
                .header("X-User-Id", 3L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmAppointment_ShouldReturnConfirmed() throws Exception {
        Appointment appointment = Appointment.builder()
                .clientId(1L).masterId(2L).serviceName("Haircut")
                .appointmentTime(LocalDateTime.now())
                .status(AppointmentStatus.PENDING).build();
        Appointment saved = appointmentRepository.save(appointment);

        mockMvc.perform(patch("/api/v1/appointments/" + saved.getId() + "/confirm")
                .header("X-User-Id", 2L)) // Мастер подтверждает
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void confirmAppointment_ShouldReturnConflict_WhenNotPending() throws Exception {
        Appointment appointment = Appointment.builder()
                .clientId(1L).masterId(2L).serviceName("Haircut")
                .appointmentTime(LocalDateTime.now())
                .status(AppointmentStatus.CONFIRMED).build(); // Уже подтверждена
        Appointment saved = appointmentRepository.save(appointment);

        mockMvc.perform(patch("/api/v1/appointments/" + saved.getId() + "/confirm")
                .header("X-User-Id", 2L))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmAppointment_ShouldReturnForbidden_WhenNotMaster() throws Exception {
        Appointment appointment = Appointment.builder()
                .clientId(1L).masterId(2L).serviceName("Haircut")
                .appointmentTime(LocalDateTime.now())
                .status(AppointmentStatus.PENDING).build();
        Appointment saved = appointmentRepository.save(appointment);

        mockMvc.perform(patch("/api/v1/appointments/" + saved.getId() + "/confirm")
                .header("X-User-Id", 1L)) // Пытается подтвердить клиент
                .andExpect(status().isConflict());
    }
}
