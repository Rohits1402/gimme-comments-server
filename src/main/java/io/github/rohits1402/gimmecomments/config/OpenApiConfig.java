package io.github.rohits1402.gimmecomments.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gimmeCommentsOpenApi() {

        return new OpenAPI()
                .info(new Info()
                        .title("GimmeComments API")
                        .version("1.0.0")
                        .description("""
                                Comments as a service. Websites embed a small script and get a
                                working comment box without building one.
                                
                                Authentication is a JWT sent as `Authorization: Bearer <token>`.
                                Obtain one from POST /api/v1/auth/login.
                                """))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")
                                        .bearerFormat("JWT")));
    }

}
