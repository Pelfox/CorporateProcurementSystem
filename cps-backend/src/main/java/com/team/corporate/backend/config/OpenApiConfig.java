package com.team.corporate.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {
    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenApiCustomizer nullableResponseFields() {
        // Shared core DTOs remain independent of Swagger annotations.
        Map<String, List<String>> nullableFields = Map.of(
                "ProductResponse", List.of("categoryId"),
                "CategoryProductCountResponse", List.of("categoryId"),
                "WarehouseResponse", List.of("location"),
                "OrderResponse", List.of("notes"),
                "ProductsStatisticsResponse", List.of("averagePrice"),
                "AuditEventResponse", List.of("actorId", "actorUsername", "entityId", "correlationId")
        );
        return api -> {
            var schemas = api.getComponents().getSchemas();
            nullableFields.forEach((name, fields) -> {
                Schema<?> schema = schemas.get(name);
                if (schema == null || schema.getProperties() == null) return;
                for (var field : fields) {
                    Schema<?> property = schema.getProperties().get(field);
                    if (property == null) continue;
                    property.setNullable(true);
                    if (api.getOpenapi() != null && !api.getOpenapi().startsWith("3.0")) {
                        var types = new LinkedHashSet<String>();
                        if (property.getTypes() != null) types.addAll(property.getTypes());
                        if (property.getType() != null) types.add(property.getType());
                        types.add("null");
                        property.setTypes(types);
                    }
                }
            });
        };
    }

    @Bean
    public OpenAPI procurementApi() {
        return new OpenAPI()
                .info(new Info().title("Система корпоративных закупок").version("1.0")
                        .description("Войдите через POST /api/auth/login и вставьте accessToken в Authorize. "
                                + "Временный пароль необходимо сменить через PUT /api/auth/password; "
                                + "после смены требуется повторный вход. Административные операции доступны ADMIN. "
                                + "Ошибки возвращаются в формате Problem Detail с Correlation ID."))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
