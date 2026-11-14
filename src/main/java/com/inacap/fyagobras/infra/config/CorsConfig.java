package com.inacap.fyagobras.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
// Configuración de seguridad que permite solicitudes del frontend durante el desarrollo.
public class CorsConfig {

    // Registra las reglas CORS que utilizará la aplicación.
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Permite el acceso a todos los endpoints desde el frontend local.
                registry.addMapping("/**")
                        // Puerto utilizado por el servidor de desarrollo del frontend.
                        .allowedOrigins("http://localhost:5173")
                        // Métodos HTTP permitidos para las solicitudes del frontend.
                        .allowedMethods("GET", "POST", "PUT", "PATCH" , "OPTIONS")
                        // Acepta cualquier encabezado enviado por el cliente.
                        .allowedHeaders("*")
                        // Permite el envío de cookies y credenciales en las solicitudes.
                        .allowCredentials(true);
            }
        };
    }
}