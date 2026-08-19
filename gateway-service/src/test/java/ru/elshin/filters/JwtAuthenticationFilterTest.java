package ru.elshin.filters;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;

    @Mock
    private GatewayFilterChain chain;

    private final String secret = "9a8f7e6d5c4b3a2f1e0d9c8b7a6f5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f";

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "secret", secret);
        lenient().when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    private String generateToken(Long userId, String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key)
                .compact();
    }

    @Test
    void filter_WhenPublicEndpointLogin_ShouldPassThroughWithoutValidation() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter gatewayFilter = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void filter_WhenPublicEndpointRegister_ShouldPassThroughWithoutValidation() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/register").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter gatewayFilter = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void filter_WhenPublicEndpointActuator_ShouldPassThroughWithoutValidation() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/health").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter gatewayFilter = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void filter_WhenMissingAuthorizationHeader_ShouldReturnUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/appointments/my").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter gatewayFilter = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_WhenInvalidAuthorizationHeaderNoBearer_ShouldReturnUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/appointments/my")
                .header(HttpHeaders.AUTHORIZATION, "Basic some-credentials")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter gatewayFilter = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_WhenInvalidToken_ShouldReturnUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/appointments/my")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.here")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilter gatewayFilter = filter.apply(new JwtAuthenticationFilter.Config());

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_WhenValidToken_ShouldEnrichRequestAndPassThrough() {
        String token = generateToken(123L, "user@test.com", "CLIENT");

        // Подставим "левые" заголовки от клиента, чтобы проверить их удаление
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/appointments/my")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-User-Id", "999")
                .header("X-User-Role", "ADMIN")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter gatewayFilter = filter.apply(new JwtAuthenticationFilter.Config());

        // Используем ArgumentCaptor для перехвата измененного exchange
        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        reset(chain); // Сбросим вызов из BeforeEach
        when(chain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
                .verifyComplete();

        ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        assertNotNull(capturedExchange);

        ServerHttpRequest capturedRequest = capturedExchange.getRequest();
        assertEquals("123", capturedRequest.getHeaders().getFirst("X-User-Id"));
        assertEquals("CLIENT", capturedRequest.getHeaders().getFirst("X-User-Role"));
    }
}
