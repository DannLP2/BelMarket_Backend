package com.productos.mari.domain.infrastructure.communication;

import com.productos.mari.domain.reservation.ReservationDto;
import com.productos.mari.domain.settings.AppSettings;
import com.productos.mari.domain.settings.AppSettingsService;
import com.productos.mari.domain.infrastructure.communication.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final AppSettingsService appSettingsService;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String fromEmail;

    @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    // Colores de Marca
    private static final String COLOR_PRIMARY = "#4f46e5"; // Indigo-600
    private static final String COLOR_SUCCESS = "#10b981"; // Emerald-500
    private static final String COLOR_DANGER = "#f43f5e";  // Rose-500
    private static final String COLOR_INFO = "#3b82f6";    // Blue-500
    private static final String COLOR_PURPLE = "#a855f7";  // Purple-500

    /**
     * Helper para envolver el contenido en una plantilla HTML profesional y responsiva.
     */
    private String buildEmail(String title, String name, String contentHtml, String ctaText, String ctaUrl, String color) {
        AppSettings settings = appSettingsService.getSettings();
        String logoHtml = (settings.getLogoUrl() != null && !settings.getLogoUrl().isEmpty())
                ? "<img src='" + settings.getLogoUrl() + "' alt='" + settings.getStoreName() + "' style='max-height: 60px; margin-bottom: 20px;'>"
                : "<h2 style='color: " + color + "; margin: 0; font-family: sans-serif; font-weight: 800;'>" + settings.getStoreName() + "</h2>";

        String ctaHtml = (ctaText != null && ctaUrl != null)
                ? "<div style='text-align: center; margin-top: 30px;'>"
                + "<a href='" + ctaUrl + "' style='background-color: " + color + "; color: white; padding: 14px 28px; text-decoration: none; border-radius: 12px; font-weight: bold; display: inline-block; font-size: 16px;'> " + ctaText + " </a>"
                + "</div>"
                : "";

        return "<html><body style='font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; background-color: #f1f5f9; padding: 20px; margin: 0;'>"
                + "<table width='100%' border='0' cellspacing='0' cellpadding='0'><tr><td align='center'>"
                + "<div style='max-width: 600px; width: 100%; margin: 20px auto; background-color: white; border-radius: 24px; overflow: hidden; box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);'>"
                // Header (Stripe)
                + "<div style='height: 8px; background-color: " + color + ";'></div>"
                + "<div style='padding: 40px 30px; text-align: center;'>"
                + logoHtml
                + "<h1 style='color: #1e293b; font-size: 24px; font-weight: 800; margin-top: 0; margin-bottom: 10px;'>" + title + "</h1>"
                + "<p style='color: #64748b; font-size: 16px; margin-bottom: 30px;'>Hola <strong>" + name + "</strong>,</p>"
                + "<div style='color: #475569; font-size: 16px; line-height: 1.6; text-align: left;'>" + contentHtml + "</div>"
                + ctaHtml
                + "<div style='margin-top: 40px; padding-top: 30px; border-top: 1px solid #f1f5f9;'>"
                + "<p style='color: #94a3b8; font-size: 14px; margin-bottom: 5px;'>" + settings.getStoreName() + "</p>"
                + "<p style='color: #cbd5e1; font-size: 12px;'>" + settings.getAddress() + "<br>" + settings.getContactEmail() + "</p>"
                + "</div>"
                + "</div>"
                + "</div>"
                + "<p style='color: #94a3b8; font-size: 12px; margin-top: 20px;'>Recibiste este correo porque eres un cliente valorado de " + settings.getStoreName() + ".</p>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    private void setupHelper(MimeMessageHelper helper, String to, String subject) throws MessagingException, java.io.UnsupportedEncodingException {
        AppSettings settings = appSettingsService.getSettings();
        helper.setTo(to);
        helper.setFrom(fromEmail, settings.getStoreName());
        helper.setSubject(subject);
    }

    @Async
    @Override
    public void sendWelcomeEmail(String toEmail, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            setupHelper(helper, toEmail, "¡Bienvenido a " + appSettingsService.getSettings().getStoreName() + "! 🎉");

            String content = "Es un honor darte la bienvenida a nuestra comunidad. Tu cuenta ha sido creada con éxito y ahora tienes acceso a cientos de productos asombrosos.<br><br>Ya puedes entrar a nuestro catálogo y explorar ofertas exclusivas diseñadas especialmente para ti.";
            String htmlBody = buildEmail("¡Te damos la bienvenida!", name, content, "Empieza a comprar ahora", frontendUrl, COLOR_PRIMARY);

            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Fallo al enviar correo de bienvenida a: " + toEmail + " | Error: " + e.getMessage());
        }
    }

    @Async
    @Override
    public void sendVerificationEmail(String toEmail, String name, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            setupHelper(helper, toEmail, "Tu Código de Verificación | " + appSettingsService.getSettings().getStoreName() + " 🔐");

            String content = "Usa el siguiente PIN de 6 dígitos para activar tu cuenta y empezar a disfrutar de nuestros servicios:"
                    + "<div style='text-align: center; margin: 30px 0;'>"
                    + "<div style='background-color: #f8fafc; border: 2px dashed #e2e8f0; border-radius: 12px; padding: 20px; font-size: 32px; letter-spacing: 8px; font-weight: 900; color: #1e293b; display: inline-block;'>" + code + "</div>"
                    + "</div>"
                    + "Este código es personal y privado. Por seguridad, **expira en 15 minutos**.";
            
            String htmlBody = buildEmail("Verifica tu cuenta", name, content, null, null, COLOR_PRIMARY);

            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Fallo al enviar correo de verificación a: " + toEmail + " | Error: " + e.getMessage());
        }
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String toEmail, String name, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            setupHelper(helper, toEmail, "Recuperación de Contraseña | " + appSettingsService.getSettings().getStoreName() + " 🔑");

            String content = "Hemos recibido una solicitud para restablecer tu contraseña. Si no fuiste tú, puedes ignorar este correo de forma segura.<br><br>Usa este código para proceder con el cambio:"
                    + "<div style='text-align: center; margin: 30px 0;'>"
                    + "<div style='background-color: #fef2f2; border: 2px solid #fecaca; border-radius: 12px; padding: 20px; font-size: 32px; letter-spacing: 8px; font-weight: 900; color: " + COLOR_DANGER + "; display: inline-block;'>" + code + "</div>"
                    + "</div>"
                    + "El código **expira en 15 minutos**.";
            
            String htmlBody = buildEmail("¿Olvidaste tu contraseña?", name, content, null, null, COLOR_DANGER);

            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Fallo al enviar correo de reseteo a: " + toEmail + " | Error: " + e.getMessage());
        }
    }

    @Async
    @Override
    public void sendReservationConfirmation(String toEmail, String name, ReservationDto reservation, byte[] pdfReceipt) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            setupHelper(helper, toEmail, "Tu Pedido #" + (reservation.getReference() != null ? reservation.getReference() : reservation.getId()) + " fue recibido | " + appSettingsService.getSettings().getStoreName() + " 🛍️");

            String content = "¡Excelentes noticias! Hemos recibido tu pedido por un valor total de <strong>" + formatCurrency(reservation.getTotal(), reservation.getDisplayCurrency()) + "</strong>.<br><br>"
                    + "Nuestro equipo comenzará a prepararlo muy pronto. Adjunto a este correo encontrarás el PDF oficial con tu recibo para tu comodidad.";
            
            String htmlBody = buildEmail("¡Pedido Confirmado!", name, content, "Ver mis pedidos", frontendUrl + "/reservations", COLOR_SUCCESS);

            helper.setText(htmlBody, true);

            if (pdfReceipt != null) {
                ByteArrayResource resource = new ByteArrayResource(pdfReceipt);
                helper.addAttachment("Comprobante_Reserva_" + (reservation.getReference() != null ? reservation.getReference() : reservation.getId()) + ".pdf", resource);
            }

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Fallo al enviar correo de reserva a: " + toEmail + " | Error: " + e.getMessage());
        }
    }

    @Async
    @Override
    public void sendReservationStatusUpdate(String toEmail, String name, String reservationReference, com.productos.mari.domain.reservation.ReservationStatus newStatus, String deliveryCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String title = "";
            String description = "";
            String color = COLOR_PRIMARY;
            String icon = "📦";

            switch (newStatus) {
                case CONFIRMED:
                    title = "¡Tu pago ha sido aprobado!";
                    description = "Hemos recibido correctamente tu pago. Ahora nuestro equipo comenzará a procesar tu pedido.";
                    color = COLOR_SUCCESS;
                    icon = "✅";
                    break;
                case PREPARING:
                    title = "Estamos preparando tu pedido";
                    description = "¡Buenas noticias! Ya estamos empacando tus productos con mucho cuidado para que lleguen perfectos.";
                    color = COLOR_PURPLE;
                    icon = "📦";
                    break;
                case SHIPPED:
                    title = "¡Tu pedido viene en camino!";
                    description = "Tu paquete ya ha salido de nuestras instalaciones y se encuentra en manos de la transportadora.";
                    if (deliveryCode != null && !deliveryCode.isEmpty()) {
                        description += "<br><br>Para recibir tu paquete, entrega este código de seguridad al repartidor:"
                                + "<div style='margin-top: 15px; font-size: 28px; font-weight: 900; color: " + COLOR_INFO + "; letter-spacing: 5px;'>" + deliveryCode + "</div>";
                    }
                    color = COLOR_INFO;
                    icon = "🚚";
                    break;
                case COMPLETED:
                    title = "¡Pedido Entregado!";
                    description = "Tu pedido ha sido marcado como entregado. ¡Esperamos que ames tus nuevos productos!";
                    color = COLOR_SUCCESS;
                    icon = "✨";
                    break;
                case CANCELLED:
                    title = "Tu pedido ha sido cancelado";
                    description = "Lamentamos informarte que tu pedido #" + reservationReference + " ha sido cancelado. Si tienes dudas, por favor contáctanos.";
                    color = COLOR_DANGER;
                    icon = "❌";
                    break;
                default:
                    return;
            }

            setupHelper(helper, toEmail, icon + " Actualización de tu pedido #" + reservationReference);

            String content = "<div style='background-color: #f8fafc; border-radius: 16px; padding: 25px; margin-bottom: 25px; border: 1px solid #e2e8f0; text-align: center;'>"
                    + "<p style='color: #1e293b; font-size: 18px; font-weight: bold; margin: 0;'>" + description + "</p>"
                    + "</div>"
                    + "Te informaremos tan pronto haya un nuevo cambio en tu envío.";

            String htmlBody = buildEmail(title, name, content, "Ver estado del pedido", frontendUrl + "/reservations", color);

            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Fallo al enviar correo de actualización para pedido #" + reservationReference + " | Error: " + e.getMessage());
        }
    }

    @Async
    @Override
    public void sendPasswordChangeNotification(String toEmail, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            setupHelper(helper, toEmail, "Tu contraseña ha sido actualizada | " + appSettingsService.getSettings().getStoreName() + " 🛡️");

            String content = "Te informamos que la contraseña de tu cuenta ha sido actualizada exitosamente.<br><br>"
                    + "Si no realizaste este cambio, por favor contacta a nuestro equipo de soporte de inmediato para proteger tu información.";
            
            String htmlBody = buildEmail("Seguridad de la Cuenta", name, content, "Ir a mi perfil", frontendUrl + "/profile", COLOR_INFO);

            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Fallo al enviar notificación de cambio de clave a: " + toEmail + " | Error: " + e.getMessage());
        }
    }

    @Async
    @Override
    public void sendAccountStatusNotification(String toEmail, String name, boolean isEnabled) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String title = isEnabled ? "Tu cuenta ha sido reactivada" : "Tu cuenta ha sido bloqueada";
            String subject = (isEnabled ? "Cuenta Activada" : "Aviso de Seguridad: Cuenta Bloqueada") + " | " + appSettingsService.getSettings().getStoreName();
            String color = isEnabled ? COLOR_SUCCESS : COLOR_DANGER;
            
            String content = isEnabled 
                ? "¡Buenas noticias! Tu cuenta ha sido reactivada y ya puedes acceder a todos nuestros servicios con normalidad."
                : "Te informamos que tu cuenta ha sido bloqueada por seguridad o por decisión administrativa. Si crees que esto es un error, por favor contacta a soporte.";
            
            setupHelper(helper, toEmail, subject + (isEnabled ? " 🎉" : " 🔒"));

            String htmlBody = buildEmail(title, name, content, isEnabled ? "Iniciar Sesión" : "Contactar Soporte", frontendUrl, color);

            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Fallo al enviar notificación de estado de cuenta a: " + toEmail + " | Error: " + e.getMessage());
        }
    }

    @Async
    @Override
    public void sendBroadcastEmail(String toEmail, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            setupHelper(helper, toEmail, subject + " | " + appSettingsService.getSettings().getStoreName() + " 📢");

            String contentHtml = content.replace("\n", "<br>");
            String htmlBody = buildEmail(subject, "Cliente", contentHtml, "Visitar Tienda", frontendUrl, COLOR_PRIMARY);

            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Fallo al enviar correo masivo a: " + toEmail + " | Error: " + e.getMessage());
        }
    }

    @Async
    @Override
    public void sendSupportNotification(com.productos.mari.domain.support.SupportRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "NUEVA SOLICITUD DE AYUDA: " + request.getRequestType();
            setupHelper(helper, fromEmail, subject + " 📩");

            StringBuilder content = new StringBuilder();
            content.append("Has recibido una nueva solicitud de soporte desde el Centro de Ayuda:<br><br>");
            content.append("<strong>Nombre:</strong> ").append(request.getName()).append("<br>");
            content.append("<strong>Email:</strong> ").append(request.getEmail()).append("<br>");
            content.append("<strong>Tipo:</strong> ").append(request.getRequestType()).append("<br>");
            
            if (request.getOrderNumber() != null && !request.getOrderNumber().isEmpty()) {
                content.append("<strong>Orden:</strong> ").append(request.getOrderNumber()).append("<br>");
            }
            
            content.append("<br><strong>Mensaje:</strong><br>").append(request.getMessage().replace("\n", "<br>")).append("<br><br>");
            
            if (request.getAttachmentUrl() != null) {
                content.append("<strong>Adjunto:</strong> <a href='").append(request.getAttachmentUrl()).append("'>Ver archivo adjunto</a><br>");
            }

            String htmlBody = buildEmail("Detalles del Ticket", "Administrador", content.toString(), null, null, COLOR_PURPLE);

            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Fallo al enviar notificación de soporte | Error: " + e.getMessage());
        }
    }

    @Async
    @Override
    public void sendSupportConfirmation(com.productos.mari.domain.support.SupportRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            setupHelper(helper, request.getEmail(), "Recibimos tu solicitud | " + appSettingsService.getSettings().getStoreName());

            String content = "Hola " + request.getName() + ",<br><br>Hemos recibido tu solicitud de soporte sobre: <strong>" + request.getRequestType() + "</strong>.<br>"
                    + "Nuestro equipo la revisará lo antes posible y te contactará por este medio.<br><br>"
                    + "Tu mensaje:<br><i>\"" + request.getMessage().replace("\n", "<br>") + "\"</i>";

            String htmlBody = buildEmail("Solicitud Recibida", request.getName(), content, "Visitar Ayuda", frontendUrl + "/help", COLOR_INFO);

            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Fallo al enviar confirmación de soporte a: " + request.getEmail());
        }
    }

    @Async
    @Override
    public void sendNewOfferEmail(com.productos.mari.domain.user.User user, com.productos.mari.domain.product.Product product, com.productos.mari.domain.marketing.Offer offer) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String discountLabel = offer.getDiscountValue().toString() + (offer.getDiscountType() == com.productos.mari.domain.marketing.Offer.DiscountType.PERCENTAGE ? "%" : "$");
            setupHelper(helper, user.getEmail(), "¡Oferta Especial: " + discountLabel + " en " + product.getName() + "! 🎁");

            String productImg = (product.getMainImageUrl() != null) 
                    ? "<div style='text-align: center; margin: 20px 0;'><img src='" + product.getMainImageUrl() + "' style='max-width: 200px; border-radius: 12px;'></div>" 
                    : "";

            String content = "¡Tenemos una oferta increíble para ti!<br><br>"
                    + "El producto <strong>" + product.getName() + "</strong> ahora tiene un descuento de <strong>" + discountLabel + "</strong>.<br>"
                    + productImg
                    + "Aprovecha esta oportunidad antes de que se agote el tiempo o el stock.";

            String htmlBody = buildEmail("¡Nueva Oferta Disponible!", user.getName(), content, "¡Aprovechar Oferta!", frontendUrl + "/product/" + product.getSlug(), COLOR_PURPLE);

            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Fallo al enviar correo de oferta a: " + user.getEmail());
        }
    }

    @Async
    @Override
    public void sendRoleUpdateNotification(String toEmail, String name, java.util.Set<com.productos.mari.domain.user.Role> roles) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            setupHelper(helper, toEmail, "Tus permisos han sido actualizados | " + appSettingsService.getSettings().getStoreName() + " 🛡️");

            StringBuilder rolesHtml = new StringBuilder("<div style='margin: 20px 0;'>");
            for (com.productos.mari.domain.user.Role role : roles) {
                String color = role == com.productos.mari.domain.user.Role.ADMIN ? COLOR_DANGER : 
                              (role == com.productos.mari.domain.user.Role.DELIVERER ? COLOR_INFO : COLOR_SUCCESS);
                String icon = role == com.productos.mari.domain.user.Role.ADMIN ? "🛡️" : 
                             (role == com.productos.mari.domain.user.Role.DELIVERER ? "🚚" : "🛍️");
                String label = role == com.productos.mari.domain.user.Role.ADMIN ? "Administrador" : 
                              (role == com.productos.mari.domain.user.Role.DELIVERER ? "Repartidor" : "Cliente");
                
                rolesHtml.append("<span style='background-color: ").append(color).append("15; color: ").append(color)
                         .append("; padding: 6px 12px; border-radius: 8px; font-weight: bold; margin-right: 10px; display: inline-block; border: 1px solid ").append(color).append("30;'>")
                         .append(icon).append(" ").append(label).append("</span>");
            }
            rolesHtml.append("</div>");

            String content = "Te informamos que un administrador ha actualizado los permisos de tu cuenta. Ahora tienes acceso a las siguientes funciones:"
                    + rolesHtml.toString()
                    + "<br>Estos cambios se han aplicado automáticamente a tu sesión actual. ¡Ya puedes empezar a usarlos!";
            
            String htmlBody = buildEmail("Actualización de Permisos", name, content, "Ir a mi panel", frontendUrl, COLOR_PRIMARY);

            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Fallo al enviar notificación de roles a: " + toEmail + " | Error: " + e.getMessage());
        }
    }

    private String formatCurrency(java.math.BigDecimal amount, String currencyCode) {
        if (amount == null) amount = java.math.BigDecimal.ZERO;
        String currency = currencyCode != null ? currencyCode : "COP";
        
        String symbol = "$";
        int decimals = 0;
        
        if ("EUR".equals(currency)) { symbol = "€"; decimals = 2; }
        else if ("GBP".equals(currency)) { symbol = "£"; decimals = 2; }
        else if ("USD".equals(currency)) { symbol = "$"; decimals = 2; }
        else if ("CAD".equals(currency) || "AUD".equals(currency)) { symbol = "A$"; decimals = 2; }
        else if (!"COP".equals(currency)) { symbol = currency + " "; decimals = 2; }

        String pattern = decimals == 0 ? "%,.0f" : "%,.2f";
        return symbol + String.format(pattern, amount);
    }
}
