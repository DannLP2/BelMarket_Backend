package com.productos.mari.domain.infrastructure.reporting;

import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.reservation.Reservation;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.reservation.ReservationRepository;
import com.productos.mari.domain.infrastructure.reporting.ReportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;

    @Override
    public ByteArrayInputStream generateInventoryReport() {
        String[] columns = {"ID", "Nombre", "Categoría", "Marca", "Stock", "Precio", "Estado"};

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inventario");

            // Style for header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // Fetch products (could also be paginated if huge, but findAll is better than findAll().stream().filter)
            List<Product> products = productRepository.findAll();

            int rowIdx = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(product.getId());
                row.createCell(1).setCellValue(product.getName());
                row.createCell(2).setCellValue(product.getCategories() != null && !product.getCategories().isEmpty() 
                                ? product.getCategories().stream().map(com.productos.mari.domain.category.Category::getName).collect(java.util.stream.Collectors.joining(", ")) : "N/A");
                row.createCell(3).setCellValue(product.getBrand() != null ? product.getBrand().getName() : "N/A");
                row.createCell(4).setCellValue(product.getStock());
                row.createCell(5).setCellValue(product.getPrice().doubleValue());
                row.createCell(6).setCellValue(Boolean.TRUE.equals(product.getIsActive()) ? "Activo" : "Inactivo");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error al generar reporte de inventario: " + e.getMessage());
        }
    }

    @Override
    public ByteArrayInputStream generateMonthlySalesReport(int month, int year) {
        String[] columns = {"ID Reserva", "Fecha", "Cliente", "Teléfono", "Total", "Estado"};

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Ventas Mensuales");

            CellStyle headerCellStyle = createHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // Fetch reservations for the month using optimized query
            LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
            LocalDateTime end = start.plusMonths(1).minusNanos(1);
            
            List<Reservation> reservations = reservationRepository.findCompletedBetween(start, end);

            double grandTotal = 0;
            int rowIdx = 1;
            for (Reservation res : reservations) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(res.getId());
                row.createCell(1).setCellValue(res.getCreatedAt().toString());
                row.createCell(2).setCellValue(res.getUser().getName());
                row.createCell(3).setCellValue(res.getUser().getPhone());
                row.createCell(4).setCellValue(res.getTotal().doubleValue());
                row.createCell(5).setCellValue(res.getStatus().toString());
                
                grandTotal += res.getTotal().doubleValue();
            }

            // Add summary row
            Row summaryRow = sheet.createRow(rowIdx + 1);
            Cell totalLabel = summaryRow.createCell(3);
            totalLabel.setCellValue("TOTAL VENTAS:");
            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);
            totalLabel.setCellStyle(boldStyle);

            summaryRow.createCell(4).setCellValue(grandTotal);

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Error al generar reporte de ventas: " + e.getMessage());
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);
        headerCellStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return headerCellStyle;
    }
}
