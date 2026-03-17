package com.sentinel.cloud_optimizer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração global de CORS.
 *
 * Por que não usar @CrossOrigin("*") no controller?
 * - "*" em produção é perigoso: qualquer domínio pode chamar sua API
 * - Configuração espalhada por vários controllers é difícil de manter
 * - Aqui centralizamos e lemos as origens permitidas de variável de ambiente
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Lista de origens separadas por vírgula.
     * Ex no .env: CORS_ALLOWED_ORIGINS=http://localhost:5500,https://meudominio.com
     * Default: localhost para desenvolvimento com Live Server do VS Code.
     */
    @Value("${cors.allowed.origins:http://localhost:5500,http://localhost:3000,http://127.0.0.1:5500}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "DELETE", "PUT", "PATCH")
                .allowedHeaders("*");
    }
}
