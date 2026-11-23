package com.inacap.fyagobras.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
// Configuración de seguridad que permite solicitudes del frontend durante el desarrollo.
public class CorsConfig {

    // CORS controla qué páginas web externas pueden llamar a los endpoints de esta API.
    // Sin estas reglas, el navegador podría bloquear una petición entre frontend y backend
    // aunque la petición fuese válida.
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        // Devuelve una configuración personalizada de Spring MVC.
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // "/**" significa que todas las rutas públicas de la API tendrán esta configuración CORS.
                registry.addMapping("/**")
                        // allowedOrigins enumera las direcciones web autorizadas.
                        // Estas dos direcciones corresponden al servidor local de Vite en los puertos 5173 y 5174.
                        .allowedOrigins("http://localhost:5173", "http://localhost:5174")
                        // allowedMethods indica qué operaciones HTTP puede realizar el frontend.
                        // OPTIONS se utiliza internamente en el navegador para comprobar CORS antes de una petición.
                        .allowedMethods("GET", "POST", "PUT", "PATCH" , "OPTIONS")
                        // allowedHeaders "*" acepta los encabezados personalizados que necesite el cliente,
                        // por ejemplo Authorization, Content-Type o Accept.
                        .allowedHeaders("*")
                        // allowCredentials true permite compartir credenciales y cookies cuando corresponda.
                        // Las credenciales no se deben combinar con un origen "*" en lugar de una lista definida.
                        .allowCredentials(true);
            }
        };
    }
}