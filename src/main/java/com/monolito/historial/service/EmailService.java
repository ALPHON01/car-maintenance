package com.monolito.historial.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Envío de correos. Si no hay servidor de correo configurado (o app.mail.activo=false)
 * la aplicación funciona igual: simplemente no se envía nada y se deja constancia en el log.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final TemplateEngine templateEngine;
    private final boolean activo;
    private final String remitente;
    private final String urlBase;

    public EmailService(ObjectProvider<JavaMailSender> mailSender,
                        TemplateEngine templateEngine,
                        @Value("${app.mail.activo:false}") boolean activo,
                        @Value("${app.mail.remitente:${spring.mail.username:}}") String remitente,
                        @Value("${app.url-base:http://localhost:8080}") String urlBase) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.activo = activo;
        this.remitente = remitente == null ? "" : remitente.trim();
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    }

    /** true si hay servidor de correo configurado y los envíos están activados. */
    public boolean estaActivo() {
        return activo && !remitente.isBlank() && mailSender.getIfAvailable() != null;
    }

    /**
     * Correo de bienvenida tras el registro. Se envía en segundo plano (@Async): si el servidor
     * de correo tarda o falla, el registro del usuario no se ve afectado.
     */
    @Async
    public void enviarBienvenida(String destinatario, String nombre) {
        if (!estaActivo()) {
            log.info("Correo de bienvenida no enviado: el envío de correos no está configurado.");
            return;
        }

        try {
            String urlLogin = urlBase + "/login";

            Context contexto = new Context(Locale.forLanguageTag("es"));
            contexto.setVariable("nombre", nombre);
            contexto.setVariable("urlLogin", urlLogin);
            String html = templateEngine.process("email/bienvenida", contexto);

            JavaMailSender sender = mailSender.getObject();
            MimeMessage mensaje = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, "Historial del coche");
            helper.setTo(destinatario);
            helper.setSubject("Te damos la bienvenida a Historial del coche");
            // Versión en texto plano para los clientes de correo que no muestran HTML
            helper.setText(textoPlano(nombre, urlLogin), html);

            sender.send(mensaje);
            log.info("Correo de bienvenida enviado");
        } catch (MessagingException | java.io.UnsupportedEncodingException | RuntimeException e) {
            // Nunca debe romper nada: solo se deja constancia
            log.error("No se ha podido enviar el correo de bienvenida", e);
        }
    }

    private String textoPlano(String nombre, String urlLogin) {
        return "Hola, " + nombre + ":\n\n"
                + "Tu cuenta en Historial del coche ya está lista. Con ella puedes añadir tus vehículos, "
                + "anotar cada visita al taller (con su factura en PDF o foto) y ver qué mantenimiento toca después.\n\n"
                + "Entra aquí: " + urlLogin + "\n\n"
                + "Si no has creado esta cuenta, puedes ignorar este mensaje.\n";
    }
}