package com.sigi.configuration.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;

import static com.sigi.util.Constants.*;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info().title("API SIGI")
                        .description("API for smart inventory management")
                        .version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }

    @Bean
    public OpenApiCustomizer addSchemasCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }

            components.addSchemas("ApiResponse", new ObjectSchema()
                    .description("Generic API response wrapper")
                    .addProperty("code", new Schema<Integer>().type("integer").format("int32").example(HttpStatus.OK.value()))
                    .addProperty("message", new Schema<String>().type("string").example(OPERATION_COMPLETED))
                    .addProperty("data", new Schema<>().type("object").nullable(true))
                    .addProperty("errors", new ArraySchema().items(new Schema<String>().type("string")).nullable(true))
                    .addProperty("timestamp", new Schema<String>().type("string").example(OffsetDateTime.now(ZoneId.of(DATE_ZONE)).toString()))
                    .addProperty("traceId", new Schema<String>().type("string").example(UUID.randomUUID()))
                    .addProperty("version", new Schema<String>().type("string").example("1.0"))
            );

            components.addSchemas("GenericErrorResponse", new ObjectSchema()
                    .description("Generic response for errors")
                    .addProperty("code", new Schema<Integer>().type("integer").format("int32").example("400 | 401 | 404 | 500"))
                    .addProperty("message", new Schema<String>().type("string").example(UNEXPECTED_ERROR))
                    .addProperty("errors", new ArraySchema().items(new Schema<String>().type("string"))
                            .example(Arrays.asList("Detailed error message 1", "Detailed error message 1")))
            );
        };

    }
}