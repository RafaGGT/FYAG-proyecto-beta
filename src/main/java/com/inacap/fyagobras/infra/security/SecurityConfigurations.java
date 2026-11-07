package com.inacap.fyagobras.infra.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configura la seguridad HTTP de la aplicación.
 *
 * <p>Utiliza JWT sin sesiones de servidor, limita el acceso a las rutas
 * públicas de autenticación y habilita la seguridad por método para que los
 * permisos por rol se validen en los endpoints protegidos.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfigurations {

    @Autowired
    private SecurityFilter securityFilter;
    @Autowired
    private RateLimitFilter rateLimitFilter;
    @Autowired
    private ErrorSeguridadHandler errorSeguridadHandler;

    /**
     * Construye la cadena de filtros y las reglas de autorización HTTP.
     *
     * <p>Las solicitudes {@code OPTIONS}, {@code POST /login} y
     * {@code POST /login/actualizar-token} son públicas. El resto requiere
     * autenticación.</p>
     *
     * @param http constructor de configuración de seguridad HTTP
     * @return cadena de filtros configurada
     * @throws Exception si Spring Security no puede construir la cadena
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorSeguridadHandler)
                        .accessDeniedHandler(errorSeguridadHandler))
                .authorizeHttpRequests(req -> {
                    req.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/login").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/login/actualizar-token").permitAll();
                    req.anyRequest().authenticated();
                })
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Expone el administrador de autenticación configurado por Spring Security.
     *
     * @param configuration configuración de autenticación de Spring
     * @return administrador de autenticación
     * @throws Exception si no puede obtenerse el administrador
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
    /**
     * Crea el codificador utilizado para almacenar y verificar contraseñas.
     *
     * @return codificador basado en BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}