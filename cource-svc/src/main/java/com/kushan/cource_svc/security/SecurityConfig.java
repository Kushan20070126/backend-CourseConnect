package com.kushan.cource_svc.security;

import com.kushan.cource_svc.security.jwt.JwtAuthenticationFilter;
import com.kushan.cource_svc.security.jwt.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final List<String> allowedOrigins;

    public SecurityConfig(JwtService jwtService,
                          @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.jwtService = jwtService;
        List<String> base = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        // Accept the ip-literal equivalent of "localhost" so requests coming from
        // http://127.0.0.1:<port> (e.g. when the dev server is bound to IPv4) are
        // not rejected with "Invalid CORS request". Without this, same-origin
        // uploads proxied through the frontend still fail because the browser
        // sends Origin: http://127.0.0.1:5173.
        List<String> expanded = new ArrayList<>(base);
        for (String o : base) {
            if (o.contains("localhost")) {
                expanded.add(o.replace("localhost", "127.0.0.1"));
            } else if (o.contains("127.0.0.1")) {
                expanded.add(o.replace("127.0.0.1", "localhost"));
            }
        }
        this.allowedOrigins = expanded;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/req/courses", "/req/courses/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/req/payments/webhook").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
