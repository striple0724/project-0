package com.taxworkbench.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI taxWorkbenchOpenApi(
            @Value("${app.runtime.public-base-url:http://localhost:19100}") String serverUrl
    ) {
        return new OpenAPI()
                .info(new Info()
                        .title("Tax Workbench Enterprise API")
                        .version("v1.2")
                        .description("대용량 세무 업무 처리, 고객사 마스터 관리, 비동기 작업 및 감사 이력(Auditing)을 위한 엔터프라이즈급 API")
                        .contact(new Contact()
                                .name("Tax Workbench Architect Team")))
                .servers(List.of(new Server().url(serverUrl).description("현재 기동 서버")));
    }
}
