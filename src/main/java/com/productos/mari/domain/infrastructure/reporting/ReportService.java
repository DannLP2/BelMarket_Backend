package com.productos.mari.domain.infrastructure.reporting;

import java.io.ByteArrayInputStream;

public interface ReportService {
    ByteArrayInputStream generateInventoryReport();
    ByteArrayInputStream generateMonthlySalesReport(int month, int year);
}
