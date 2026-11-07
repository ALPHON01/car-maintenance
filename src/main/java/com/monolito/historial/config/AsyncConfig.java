package com.monolito.historial.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Activa @Async: el envío de correos se hace en segundo plano y no retrasa la respuesta al usuario. */
@Configuration
@EnableAsync
public class AsyncConfig {
}