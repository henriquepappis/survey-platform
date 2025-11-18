package com.survey.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI surveyApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Survey API")
                        .description("API para criação e gestão de pesquisas, perguntas e opções.")
                        .version("v1")
                        .contact(new Contact().name("Equipe Survey").email("contato@survey.com"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Documentação do projeto")
                        .url("https://github.com/henrique/survey-api"));
    }
}
