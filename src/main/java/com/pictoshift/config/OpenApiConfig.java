package com.pictoshift.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Общие сведения об API для Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pictoShiftOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PictoShift API")
                .description("REST API для конвертации изображений между форматами.")
                .version("1.0.0"));
    }
}
