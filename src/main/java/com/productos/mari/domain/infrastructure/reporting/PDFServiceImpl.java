package com.productos.mari.domain.infrastructure.reporting;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.productos.mari.domain.reservation.ReservationDto;
import com.productos.mari.domain.reservation.ReservationItemDto;
import com.productos.mari.domain.settings.AppSettings;
import com.productos.mari.domain.settings.AppSettingsService;
import com.productos.mari.domain.infrastructure.reporting.PDFService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.awt.Color;
import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PDFServiceImpl implements PDFService {

    private final AppSettingsService appSettingsService;

    @Value("${app.client.url:http://localhost:4200}")
    private String clientBaseUrl;

    // Colores de Marca (BelMarket Indigo)
    private static final Color MAIN_COLOR = new Color(79, 70, 229); // Indigo-600
    private static final Color TEXT_DARK = new Color(17, 24, 39); // Gray-900
    private static final Color TEXT_MUTED = new Color(107, 114, 128); // Gray-500
    private static final Color BACKGROUND_STRIPE = new Color(249, 250, 251); // Gray-50

    @Override
    public byte[] generateReservationReceipt(ReservationDto reservation) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();

        try {
            AppSettings settings = appSettingsService.getSettings();
            
            String statusText = getStatusTranslation(reservation.getStatus());
            Color statusColor = getStatusColor(reservation.getStatus());
            
            PdfWriter writer = PdfWriter.getInstance(document, out);
            
            // --- HEADER DATA PREPARATION ---
            String prefix = settings.getStoreName() != null && settings.getStoreName().length() >= 3 
                ? settings.getStoreName().substring(0, 3).toUpperCase() 
                : "BEL";
            String orderRef = reservation.getReference() != null ? reservation.getReference() : prefix + "-" + String.format("%06d", reservation.getId());
            String documentTitle = reservation.getStatus() == com.productos.mari.domain.reservation.ReservationStatus.COMPLETED ? "FACTURA" : "COMPROBANTE";

            // --- FONTS (Define early for Page Event) ---
            Font fontFooter = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_MUTED);
            
            // Set Page Event for persistent footer/header and watermark
            writer.setPageEvent(new HeaderFooterPageEvent(settings, fontFooter, statusColor, statusText, documentTitle, orderRef));
            
            document.setMargins(36, 36, 36, 70); // Space for footer
            document.open();

            // --- FONTS ---
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE);
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, statusColor);
            Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXT_MUTED);
            Font fontValue = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT_DARK);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_DARK);
            Font fontTableHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            // fontFooter defined above
            Font fontBadge = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font fontTiny = FontFactory.getFont(FontFactory.HELVETICA, 7, TEXT_MUTED);

            // --- HEADER BANNER ---
            PdfPTable topBanner = new PdfPTable(3);
            topBanner.setWidthPercentage(100f);
            topBanner.setWidths(new float[]{1f, 5.5f, 2.5f}); // Logo, Title, Ref/Status

            // 1. Logo Section
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBackgroundColor(statusColor);
            logoCell.setBorder(PdfPCell.NO_BORDER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            logoCell.setPadding(10f);
            
            boolean hasLogo = false;
            if (settings.getLogoUrl() != null && !settings.getLogoUrl().isEmpty()) {
                try {
                    String logoUrlStr = settings.getLogoUrl();
                    if (logoUrlStr.contains("res.cloudinary.com") && logoUrlStr.contains("/upload/")) {
                        if (!logoUrlStr.contains("f_png")) {
                            logoUrlStr = logoUrlStr.replace("/upload/", "/upload/f_png/");
                        }
                    }
                    
                    java.net.URL url = new java.net.URL(logoUrlStr);
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    
                    try (java.io.InputStream in = connection.getInputStream();
                         java.io.ByteArrayOutputStream outStream = new java.io.ByteArrayOutputStream()) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            outStream.write(buffer, 0, bytesRead);
                        }
                        com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(outStream.toByteArray());
                        logo.scaleToFit(38f, 38f); 
                        
                        logoCell = new PdfPCell(logo, false);
                        logoCell.setBackgroundColor(statusColor);
                        logoCell.setBorder(PdfPCell.NO_BORDER);
                        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        logoCell.setPadding(10f);
                        hasLogo = true;
                    }
                } catch (Exception e) {
                    System.err.println("Error loading PDF logo: " + e.getMessage());
                }
            }
            topBanner.addCell(logoCell);

            // 2. Title Section
            PdfPCell titleCell = new PdfPCell(new Phrase(documentTitle, fontHeader));
            titleCell.setBackgroundColor(statusColor);
            titleCell.setBorder(PdfPCell.NO_BORDER);
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE); // Vertically centered perfectly
            titleCell.setHorizontalAlignment(Element.ALIGN_LEFT); // Text immediately next to logo column
            titleCell.setPaddingTop(10f);
            titleCell.setPaddingBottom(10f);
            titleCell.setPaddingLeft(0f); // Stick close to logo column
            topBanner.addCell(titleCell);

            // 3. Reference and Status
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM, yyyy HH:mm", new Locale("es", "CO"));
            String dateStr = reservation.getCreatedAt() != null ? reservation.getCreatedAt().format(formatter) : "---";

            Font fontBannerRef = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            Font fontBannerStatus = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.WHITE);
            Font fontBannerDate = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.WHITE);
            
            PdfPCell refCell = new PdfPCell();
            refCell.setBackgroundColor(statusColor);
            refCell.setBorder(PdfPCell.NO_BORDER);
            refCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            refCell.setPadding(10f);
            Paragraph pRef = new Paragraph();
            pRef.setAlignment(Element.ALIGN_RIGHT);
            pRef.add(new Phrase("Orden: " + orderRef + "\n", fontBannerRef));
            pRef.add(new Phrase(statusText.toUpperCase() + "\n", fontBannerStatus));
            pRef.add(new Phrase(dateStr, fontBannerDate));
            refCell.addElement(pRef);
            topBanner.addCell(refCell);

            document.add(topBanner);
            document.add(new Paragraph(" "));

            // --- INFO GRID ---
            PdfPTable infoGrid = new PdfPTable(2);
            infoGrid.setWidthPercentage(100f);
            infoGrid.setSpacingAfter(20f);

            // Left: Empty space where store name used to be
            PdfPCell storeCell = new PdfPCell();
            storeCell.setBorder(PdfPCell.NO_BORDER);
            infoGrid.addCell(storeCell);

            // Right: Meta info
            PdfPCell metaCell = new PdfPCell();
            metaCell.setBorder(PdfPCell.NO_BORDER);
            metaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            Paragraph pDate = new Paragraph();
            pDate.setAlignment(Element.ALIGN_RIGHT);
            
            if (reservation.getStatus() == com.productos.mari.domain.reservation.ReservationStatus.PENDING && reservation.getCreatedAt() != null) {
                String dueDateStr = reservation.getCreatedAt().plusDays(1).format(formatter);
                pDate.add(new Phrase("FECHA LÍMITE DE PAGO\n", fontLabel));
                pDate.add(new Phrase(dueDateStr + "\n\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(220, 38, 38))));
            }
            metaCell.addElement(pDate);

            infoGrid.addCell(metaCell);
            document.add(infoGrid);

            boolean isCancelled = reservation.getStatus() == com.productos.mari.domain.reservation.ReservationStatus.CANCELLED;

            // --- CUSTOMER & PAYMENT GRID ---
            PdfPTable clientPaymentTable = new PdfPTable(isCancelled ? 1 : 2);
            clientPaymentTable.setWidthPercentage(100f);
            clientPaymentTable.setSpacingAfter(25f);

            // Customer Box
            PdfPCell clientBox = new PdfPCell();
            clientBox.setPadding(12f);
            clientBox.setBorderColor(new Color(229, 231, 235));
            clientBox.setBorderWidth(0.5f);
            clientBox.addElement(new Paragraph("CLIENTE", fontLabel));
            clientBox.addElement(new Paragraph(reservation.getCustomerName().toUpperCase(), fontValue));
            clientBox.addElement(new Paragraph(reservation.getCustomerEmail(), fontNormal));
            if (reservation.getCustomerPhone() != null) {
                clientBox.addElement(new Paragraph("Tel: " + reservation.getCustomerPhone(), fontNormal));
            }
            clientPaymentTable.addCell(clientBox);

            // Payment Box
            if (!isCancelled) {
                PdfPCell paymentBox = new PdfPCell();
                paymentBox.setPadding(12f);
                paymentBox.setBorderColor(new Color(229, 231, 235));
                paymentBox.setBorderWidth(0.5f);
                paymentBox.addElement(new Paragraph("INFORMACIÓN DE PAGO", fontLabel));
                
                String proveedor = reservation.getPaymentMethod() != null ? reservation.getPaymentMethod() : "PENDIENTE";
                String medioPago = reservation.getPaymentSubMethod() != null ? reservation.getPaymentSubMethod() : "No especificado";
                
                paymentBox.addElement(new Paragraph("Proveedor: " + proveedor.toUpperCase(), fontValue));
                if (!"PENDIENTE".equalsIgnoreCase(proveedor) && !"LOCAL_PICKUP".equalsIgnoreCase(proveedor)) {
                    paymentBox.addElement(new Paragraph("Medio de Pago: " + medioPago.toUpperCase(), fontNormal));
                    DateTimeFormatter paymentFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                    String paymentDateStr = reservation.getCreatedAt() != null ? reservation.getCreatedAt().format(paymentFormatter) : "---";
                    paymentBox.addElement(new Paragraph("Fecha de Pago: " + paymentDateStr, fontNormal));
                }
                if (reservation.getPaymentId() != null) {
                    paymentBox.addElement(new Paragraph("Transacción: " + reservation.getPaymentId(), fontNormal));
                }
                clientPaymentTable.addCell(paymentBox);
            }
            document.add(clientPaymentTable);

            // Shipping Info (Full Width / Two Columns)
            if (!isCancelled) {
                if (reservation.getShippingAddress() != null && !reservation.getShippingAddress().isEmpty()) {
                    PdfPTable shipTable = new PdfPTable(2);
                    shipTable.setWidthPercentage(100f);
                    shipTable.setWidths(new float[]{1.2f, 0.8f});
                    shipTable.setSpacingAfter(20f);
                    
                    // Address Box
                    PdfPCell sCell = new PdfPCell();
                    sCell.setPadding(12f);
                    sCell.setBorderColor(new Color(229, 231, 235));
                    sCell.setBorderWidth(0.5f);
                    sCell.addElement(new Paragraph("DIRECCIÓN DE ENTREGA", fontLabel));
                    
                    String cleanAddress = reservation.getShippingAddress();
                    cleanAddress = cleanAddress.split(" - Recibe:")[0].split(" - Telf:")[0];
                    
                    sCell.addElement(new Paragraph(cleanAddress, fontNormal));
                    if (reservation.getNeighborhood() != null && !cleanAddress.contains(reservation.getNeighborhood())) {
                        sCell.addElement(new Paragraph("Barrio/Sector: " + reservation.getNeighborhood(), fontNormal));
                    }
                    shipTable.addCell(sCell);

                    // Receiver Box
                    PdfPCell rCell = new PdfPCell();
                    rCell.setPadding(12f);
                    rCell.setBorderColor(new Color(229, 231, 235));
                    rCell.setBorderWidth(0.5f);
                    rCell.addElement(new Paragraph("RECIBE", fontLabel));
                    rCell.addElement(new Paragraph(reservation.getReceiverName() != null ? reservation.getReceiverName().toUpperCase() : reservation.getCustomerName().toUpperCase(), fontValue));
                    if (reservation.getReceiverPhone() != null) {
                        rCell.addElement(new Paragraph("Tel: " + reservation.getReceiverPhone(), fontNormal));
                    }
                    shipTable.addCell(rCell);
                    
                    document.add(shipTable);
                } else {
                    // Pickup Info
                    PdfPTable pickupTable = new PdfPTable(1);
                    pickupTable.setWidthPercentage(100f);
                    pickupTable.setSpacingAfter(20f);
                    PdfPCell pCell = new PdfPCell();
                    pCell.setPadding(12f);
                    pCell.setBorderColor(new Color(229, 231, 235));
                    pCell.addElement(new Paragraph("MÉTODO DE ENTREGA", fontLabel));
                    pCell.addElement(new Paragraph("RECOGIDA EN TIENDA", fontValue));
                    pCell.addElement(new Paragraph(settings.getAddress(), fontNormal));
                    pickupTable.addCell(pCell);
                    document.add(pickupTable);
                }
            }

            // --- ITEMS TABLE ---
            PdfPTable itemsTable = new PdfPTable(4);
            itemsTable.setWidthPercentage(100f);
            itemsTable.setWidths(new float[]{4.5f, 1f, 2f, 2.5f});
            itemsTable.setSpacingAfter(15f);
            itemsTable.setHeaderRows(1); // Repeat header on new pages

            Font fontTableHeadLight = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_DARK);
            Color headerBgColor = new Color(249, 250, 251);
            Color borderLineColor = new Color(243, 244, 246);

            String[] headers = {"Producto", "Cant", "Precio", "Subtotal"};
            for (String hText : headers) {
                PdfPCell h = new PdfPCell(new Phrase(hText, fontTableHeadLight));
                h.setBackgroundColor(headerBgColor);
                h.setPadding(10f);
                h.setPaddingTop(12f);
                h.setPaddingBottom(12f);
                h.setBorder(PdfPCell.BOTTOM);
                h.setBorderWidthBottom(1f);
                h.setBorderColorBottom(borderLineColor);
                h.setHorizontalAlignment(Element.ALIGN_LEFT);
                itemsTable.addCell(h);
            }

            for (ReservationItemDto item : reservation.getItems()) {
                PdfPCell c1 = new PdfPCell(new Paragraph(item.getProductName(), fontNormal));
                c1.setPadding(10f);
                c1.setPaddingTop(12f);
                c1.setPaddingBottom(12f);
                c1.setBorder(PdfPCell.BOTTOM); 
                c1.setBorderColor(borderLineColor);
                c1.setHorizontalAlignment(Element.ALIGN_LEFT);
                itemsTable.addCell(c1);

                PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), fontNormal));
                c2.setPadding(10f);
                c2.setPaddingTop(12f);
                c2.setPaddingBottom(12f);
                c2.setBorder(PdfPCell.BOTTOM); 
                c2.setBorderColor(borderLineColor);
                c2.setHorizontalAlignment(Element.ALIGN_LEFT);
                itemsTable.addCell(c2);

                PdfPCell c3 = new PdfPCell(new Phrase(formatCurrency(item.getPrice(), reservation.getDisplayCurrency()), fontNormal));
                c3.setPadding(10f);
                c3.setPaddingTop(12f);
                c3.setPaddingBottom(12f);
                c3.setBorder(PdfPCell.BOTTOM); 
                c3.setBorderColor(borderLineColor);
                c3.setHorizontalAlignment(Element.ALIGN_LEFT);
                itemsTable.addCell(c3);

                BigDecimal sub = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                PdfPCell c4 = new PdfPCell(new Phrase(formatCurrency(sub, reservation.getDisplayCurrency()), fontNormal));
                c4.setPadding(10f);
                c4.setPaddingTop(12f);
                c4.setPaddingBottom(12f);
                c4.setBorder(PdfPCell.BOTTOM); 
                c4.setBorderColor(borderLineColor);
                c4.setHorizontalAlignment(Element.ALIGN_LEFT);
                itemsTable.addCell(c4);
            }
            document.add(itemsTable);
            
            // --- MASTER FOOTER ANCHORING ---
            PdfPTable masterFooter = new PdfPTable(1);
            masterFooter.setTotalWidth(document.right() - document.left());
            masterFooter.setLockedWidth(true);

            // 1. Summary & QR Grid
            PdfPTable footerGrid = new PdfPTable(2);
            footerGrid.setWidthPercentage(100f);
            footerGrid.setWidths(new float[]{1f, 1f});
            
            PdfPCell qrBox = new PdfPCell();
            qrBox.setPadding(10f);
            qrBox.setBorderColor(new Color(229, 231, 235));
            qrBox.setBorderWidth(0.5f);
            qrBox.setVerticalAlignment(Element.ALIGN_TOP);
            try {
                String trackingUrl = clientBaseUrl + "/reservations";
                if (reservation.getReference() != null) {
                    trackingUrl += "?ref=" + reservation.getReference();
                }
                byte[] qrBytes = generateQRCode(trackingUrl, 150, 150);
                Image qrImage = Image.getInstance(qrBytes);
                qrImage.scaleToFit(85f, 85f);
                qrImage.setAlignment(Image.ALIGN_CENTER);
                Paragraph pQrTitle = new Paragraph("RASTREO DE PEDIDO", fontLabel);
                pQrTitle.setAlignment(Element.ALIGN_CENTER);
                pQrTitle.setSpacingAfter(0f);
                qrBox.addElement(pQrTitle);
                qrImage.setSpacingBefore(8f);
                qrBox.addElement(qrImage);
            } catch (Exception e) {}
            footerGrid.addCell(qrBox);

            PdfPCell summaryBox = new PdfPCell();
            summaryBox.setPadding(10f);
            summaryBox.setBorderColor(new Color(229, 231, 235));
            summaryBox.setBorderWidth(0.5f);
            summaryBox.setVerticalAlignment(Element.ALIGN_TOP); 
            Paragraph pSumTitle = new Paragraph("RESUMEN DE PAGO", fontLabel);
            pSumTitle.setAlignment(Element.ALIGN_CENTER);
            pSumTitle.setSpacingAfter(0f);
            summaryBox.addElement(pSumTitle);
            
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(90f); 
            totalTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            totalTable.setSpacingBefore(30f);
            
            BigDecimal subtotal = reservation.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalTable.addCell(createLabelCell("Subtotal:", fontNormal));
            totalTable.addCell(createValueCell(formatCurrency(subtotal, reservation.getDisplayCurrency()), fontNormal));
            
            if (reservation.getShippingCost() != null && reservation.getShippingCost().doubleValue() > 0) {
                totalTable.addCell(createLabelCell("Envío:", fontNormal));
                totalTable.addCell(createValueCell(formatCurrency(reservation.getShippingCost(), reservation.getDisplayCurrency()), fontNormal));
            }
            if (reservation.getTaxAmount() != null && reservation.getTaxAmount().doubleValue() > 0) {
                totalTable.addCell(createLabelCell("Impuestos:", fontNormal));
                totalTable.addCell(createValueCell(formatCurrency(reservation.getTaxAmount(), reservation.getDisplayCurrency()), fontNormal));
            }
            totalTable.addCell(createLabelCell("TOTAL", fontTitle));
            totalTable.addCell(createValueCell(formatCurrency(reservation.getTotal(), reservation.getDisplayCurrency()), fontTitle));
            
            int totalItems = reservation.getItems().stream().mapToInt(ReservationItemDto::getQuantity).sum();
            PdfPCell countCell = new PdfPCell(new Phrase("Artículos totales: " + totalItems, fontTiny));
            countCell.setBorder(PdfPCell.NO_BORDER);
            countCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            countCell.setColspan(2);
            countCell.setPaddingTop(3f);
            totalTable.addCell(countCell);
            summaryBox.addElement(totalTable);
            footerGrid.addCell(summaryBox);

            PdfPCell gridCell = new PdfPCell(footerGrid);
            gridCell.setBorder(PdfPCell.NO_BORDER);
            gridCell.setPaddingBottom(15f);
            masterFooter.addCell(gridCell);

            // 2. Signature & Policies
            PdfPTable bottomTable = new PdfPTable(2);
            bottomTable.setWidthPercentage(100f);
            bottomTable.setWidths(new float[]{1.2f, 0.8f});
            
            PdfPCell termsCell = new PdfPCell();
            termsCell.setBorder(PdfPCell.NO_BORDER);
            termsCell.addElement(new Paragraph("NOTA DE CALIDAD", fontLabel));
            termsCell.addElement(new Paragraph("En " + settings.getStoreName() + " nos comprometemos con la frescura y calidad de cada producto. Esta reserva garantiza la disponibilidad de tus artículos para la entrega programada.", fontTiny));
            bottomTable.addCell(termsCell);

            PdfPCell signCell = new PdfPCell();
            signCell.setBorder(PdfPCell.NO_BORDER);
            signCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
            Paragraph pSign = new Paragraph("__________________________\n", fontNormal);
            pSign.setAlignment(Element.ALIGN_RIGHT);
            signCell.addElement(pSign);
            Paragraph pSignLabel = new Paragraph("Firma de Conformidad (Recibido)", fontLabel);
            pSignLabel.setAlignment(Element.ALIGN_RIGHT);
            signCell.addElement(pSignLabel);
            bottomTable.addCell(signCell);
            
            PdfPCell bottomCell = new PdfPCell(bottomTable);
            bottomCell.setBorder(PdfPCell.NO_BORDER);
            bottomCell.setPaddingBottom(15f);
            masterFooter.addCell(bottomCell);

            // 3. Contact Info
            PdfPTable contactTable = new PdfPTable(3);
            contactTable.setWidthPercentage(100f);
            contactTable.addCell(createContactCell("Email", settings.getContactEmail(), fontLabel, fontNormal));
            contactTable.addCell(createContactCell("Teléfono", settings.getContactPhone(), fontLabel, fontNormal));
            contactTable.addCell(createContactCell("WhatsApp", settings.getWhatsappNumber(), fontLabel, fontNormal));
            
            PdfPCell contactCellWrap = new PdfPCell(contactTable);
            contactCellWrap.setBorder(PdfPCell.TOP);
            contactCellWrap.setBorderColor(new Color(229, 231, 235));
            contactCellWrap.setPaddingTop(10f);
            masterFooter.addCell(contactCellWrap);

            // 4. Footer Text
            Paragraph fText = new Paragraph(settings.getFooterText() != null ? settings.getFooterText() : "", fontFooter);
            fText.setAlignment(Element.ALIGN_CENTER);
            PdfPCell footerTextCell = new PdfPCell();
            footerTextCell.setBorder(PdfPCell.NO_BORDER);
            footerTextCell.setPaddingTop(10f);
            footerTextCell.addElement(fText);
            masterFooter.addCell(footerTextCell);

            // --- FINAL RENDER ---
            float totalH = masterFooter.getTotalHeight();
            // Spacer to ensure page break if items reach the bottom
            PdfPTable spacer = new PdfPTable(1);
            spacer.setTotalWidth(document.right() - document.left());
            PdfPCell sCell = new PdfPCell();
            sCell.setFixedHeight(totalH + 20f);
            sCell.setBorder(PdfPCell.NO_BORDER);
            spacer.addCell(sCell);
            document.add(spacer);

            // Pin to absolute bottom
            masterFooter.writeSelectedRows(0, -1, document.leftMargin(), document.bottomMargin() + totalH, writer.getDirectContent());

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    // cleanSocialHandle removed as it's no longer used

    private PdfPCell createLabelCell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(PdfPCell.NO_BORDER);
        c.setPaddingTop(5f);
        return c;
    }

    private PdfPCell createValueCell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(PdfPCell.NO_BORDER);
        c.setPaddingTop(5f);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private PdfPCell createContactCell(String label, String value, Font labelFont, Font valueFont) {
        PdfPCell c = new PdfPCell();
        c.setBorder(PdfPCell.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        if (value != null && !value.isEmpty()) {
            Paragraph p = new Paragraph();
            p.setAlignment(Element.ALIGN_CENTER);
            p.add(new Phrase(label.toUpperCase() + ": ", labelFont));
            p.add(new Phrase(value, valueFont));
            c.addElement(p);
        }
        return c;
    }

    private byte[] generateQRCode(String text, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        java.util.Map<com.google.zxing.EncodeHintType, Object> hints = new java.util.HashMap<>();
        hints.put(com.google.zxing.EncodeHintType.MARGIN, 1); // Reduce quiet zone (border)
        
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);
        
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", pngOutputStream);
        return pngOutputStream.toByteArray();
    }

    // --- PAGE EVENT FOR MULTI-PAGE SUPPORT ---
    private class HeaderFooterPageEvent extends PdfPageEventHelper {
        private final AppSettings settings;
        private final Font fontFooter;
        private final Color statusColor;
        private final String watermarkText;
        private final String documentTitle;
        private final String orderRef;

        public HeaderFooterPageEvent(AppSettings settings, Font fontFooter, Color statusColor, String watermarkText, String documentTitle, String orderRef) {
            this.settings = settings;
            this.fontFooter = fontFooter;
            this.statusColor = statusColor;
            this.watermarkText = watermarkText;
            this.documentTitle = documentTitle;
            this.orderRef = orderRef;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            addWatermark(writer);
            
            PdfPTable footer = new PdfPTable(3);
            try {
                footer.setWidths(new float[]{2, 1, 2});
                footer.setTotalWidth(527);
                footer.setLockedWidth(true);
                footer.getDefaultCell().setFixedHeight(20);
                footer.getDefaultCell().setBorder(Rectangle.TOP);
                footer.getDefaultCell().setBorderColor(new Color(229, 231, 235));

                // Page number
                PdfPCell pCell = new PdfPCell(new Phrase("Página " + writer.getPageNumber(), fontFooter));
                pCell.setBorder(Rectangle.TOP);
                pCell.setBorderColor(new Color(229, 231, 235));
                pCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                footer.addCell(pCell);

                // Store Name (Center)
                PdfPCell sCell = new PdfPCell(new Phrase(settings.getStoreName(), fontFooter));
                sCell.setBorder(Rectangle.TOP);
                sCell.setBorderColor(new Color(229, 231, 235));
                sCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                footer.addCell(sCell);

                // Document type
                PdfPCell dCell = new PdfPCell(new Phrase(documentTitle + " " + orderRef, fontFooter));
                dCell.setBorder(Rectangle.TOP);
                dCell.setBorderColor(new Color(229, 231, 235));
                dCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                footer.addCell(dCell);

                footer.writeSelectedRows(0, -1, 34, 40, writer.getDirectContent());
            } catch (Exception e) {
                // ignore
            }
        }

        private void addWatermark(PdfWriter writer) {
            if (watermarkText == null || watermarkText.isEmpty()) return;
            
            // Draw Over instead of Under to be visible above colored backgrounds, 
            // but use very low opacity (15-20)
            PdfContentByte canvas = writer.getDirectContent(); 
            Font fontWatermark = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 70, new Color(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), 25));
            
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, 
                new Phrase(watermarkText.toUpperCase(), fontWatermark), 
                297, 421, 45); 
        }
    }

    private Color getStatusColor(com.productos.mari.domain.reservation.ReservationStatus status) {
        if (status == null) return MAIN_COLOR;
        switch (status) {
            case COMPLETED: return new Color(22, 163, 74); // Green-600
            case CANCELLED: return new Color(220, 38, 38); // Red-600
            case CONFIRMED: return new Color(37, 99, 235); // Blue-600
            case PREPARING: return new Color(217, 119, 6); // Amber-600
            case SHIPPED:   return new Color(124, 58, 237); // Violet-600
            default:        return MAIN_COLOR; // Indigo (PENDING or others)
        }
    }

    private String getStatusTranslation(com.productos.mari.domain.reservation.ReservationStatus status) {
        if (status == null) return "PENDIENTE";
        switch (status) {
            case PENDING:   return "PENDIENTE";
            case CONFIRMED: return "CONFIRMADO";
            case PREPARING: return "EN PREPARACIÓN";
            case SHIPPED:   return "ENVIADO";
            case COMPLETED: return "COMPLETADO";
            case CANCELLED: return "CANCELADO";
            default:        return status.toString();
        }
    }

    private String formatCurrency(java.math.BigDecimal amount, String currencyCode) {
        if (amount == null) amount = java.math.BigDecimal.ZERO;
        String currency = currencyCode != null ? currencyCode : "COP";
        
        // Symbols mapping
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
