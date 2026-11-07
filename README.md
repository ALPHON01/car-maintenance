# Historial del coche

Aplicación web para llevar el mantenimiento de tus vehículos: registra cada visita al taller
(con la factura en PDF o foto) y consulta qué revisión toca después.

Spring Boot 4 · Java 21 · Spring Security · Thymeleaf · Bootstrap · MySQL

## Funciones
- Registro e inicio de sesión de usuarios (cada uno ve solo sus datos)
- Vehículos, talleres y "taller de confianza"
- Historial de intervenciones con adjuntos y próximos mantenimientos
- Correo de bienvenida al registrarse

## Puesta en marcha
1. Crea la base de datos: `CREATE DATABASE car_maintenance CHARACTER SET utf8mb4;`
2. `mvn spring-boot:run` y abre http://localhost:8080