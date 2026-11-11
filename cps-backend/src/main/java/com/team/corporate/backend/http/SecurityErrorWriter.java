package com.team.corporate.backend.http;

import com.team.corporate.backend.dto.ApiErrorDto;
import com.team.corporate.backend.security.SecurityAudit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;

@Component
public class SecurityErrorWriter {
    private final ObjectMapper mapper;
    private final Clock clock;

    public SecurityErrorWriter(ObjectMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        if (status == 401) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        SecurityAudit.record("HTTP_ACCESS", SecurityAudit.currentActor(), null, Integer.toString(status));
        mapper.writeValue(response.getWriter(), new ApiErrorDto(clock.instant(), status,
                HttpStatus.valueOf(status).getReasonPhrase(), message, request.getRequestURI(), List.of()));
    }
}
