package com.monolito.historial.config;

import com.monolito.historial.security.AuthFailureHandler;
import com.monolito.historial.security.LoginSuccessHandler;
import com.monolito.historial.security.LogoutSesionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            AuthFailureHandler authFailureHandler,
                                            LoginSuccessHandler loginSuccessHandler,
                                            LogoutSesionHandler logoutSesionHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Públicas: portada, login, registro y recursos estáticos
                        .requestMatchers("/", "/login", "/registro", "/error", "/favicon.ico",
                                "/css/**", "/js/**", "/img/**").permitAll()
                        // Todo lo demás exige haber iniciado sesión
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(loginSuccessHandler)   // crea las variables de sesión
                        .failureHandler(authFailureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .addLogoutHandler(logoutSesionHandler) // elimina las variables de sesión
                        .logoutSuccessUrl("/?logout")
                        .deleteCookies("JSESSIONID")
                        .permitAll());

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
