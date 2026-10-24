package com.trustvault.security;

import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.web.cors.*;
import java.util.*;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
    @Bean SecurityFilterChain filterChain(HttpSecurity http,JwtAuthenticationFilter jwt) throws Exception {
        return http.csrf(c->c.disable()).cors(c->c.configurationSource(cors())).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType(MediaType.APPLICATION_JSON_VALUE);res.getWriter().write("{\"error\":\"Authentication required\"}");})
                .accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType(MediaType.APPLICATION_JSON_VALUE);res.getWriter().write("{\"error\":\"Forbidden\"}");}))
            .authorizeHttpRequests(a->a.requestMatchers("/api/auth/**","/actuator/health").permitAll().anyRequest().authenticated())
            .addFilterBefore(jwt,org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class).build();
    }
    @Bean CorsConfigurationSource cors(){CorsConfiguration c=new CorsConfiguration();c.setAllowedOrigins(List.of("http://localhost:5173","http://localhost:3000"));c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));c.setAllowedHeaders(List.of("*"));c.setAllowCredentials(false);UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return s;}
}
