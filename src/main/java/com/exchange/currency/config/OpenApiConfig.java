package com.exchange.currency.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Currency Exchange Rates Provider API",
                version = "1.0.0",
                description = """
                        Multi-provider currency exchange rates aggregation service.
                        
                        This API provides access to:
                        - Currency management
                        - Real-time exchange rate calculations
                        - Exchange rate trends analysis
                        
                        **Authentication:**
                        - Public endpoints: GET /currencies, GET /exchange-rates
                        - ADMIN only: POST /currencies, POST /refresh
                        - ADMIN + PREMIUM_USER: GET /trends
                        
                        **Test Users:**
                        - admin/admin123 (ADMIN role)
                        - premium/premium123 (PREMIUM_USER role)
                        - user/user123 (USER role)
                        """,
                contact = @Contact(
                        name = "Development Team",
                        email = "dev@example.com"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local server")
        }
)
@SecurityScheme(
        name = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic"
)
public class OpenApiConfig {
}
