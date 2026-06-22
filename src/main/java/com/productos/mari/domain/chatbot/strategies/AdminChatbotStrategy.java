package com.productos.mari.domain.chatbot.strategies;

import com.productos.mari.domain.infrastructure.admin.AdminReportingService;
import com.productos.mari.domain.settings.AppSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AdminChatbotStrategy implements ChatbotRoleStrategy {

    private final AdminReportingService adminReportingService;

    @Override
    public boolean supports(String role) {
        return "ADMIN".equalsIgnoreCase(role);
    }

    @Override
    public String getSystemPrompt(String message, AppSettings settings) {
        var stats = adminReportingService.getAdminStats();
        
        // 1. Financial Snapshot
        String financialCtx = String.format("""
            == ESTADO FINANCIERO ==
            - Ingresos Totales: $%.2f
            - Ingresos este Mes: $%.2f (Crecimiento: %.2f%%)
            - Utilidad Neta: $%.2f
            - Inversion en Stock: $%.2f
            """, stats.getTotalRevenue(), stats.getMonthlyRevenue(), stats.getRevenueGrowth(), 
                 stats.getTotalProfit(), stats.getTotalInvestment());

        // 2. Operational Snapshot
        String operationalCtx = String.format("""
            == OPERACIONES Y SALUD ==
            - Pedidos Pendientes: %d
            - Alertas de Stock Bajo: %d
            - Tickets de Soporte Pendientes: %d
            - Alertas de Seguridad (48h): %d
            - Usuarios Registrados: %d
            """, stats.getPendingOrders(), stats.getLowStockCount(), 
                 stats.getPendingSupportTicketsCount(), stats.getSecurityAlertsCount(), stats.getTotalUsers());

        // 3. Top Performers
        String topProductsCtx = stats.getTopProducts().stream()
                .map(p -> String.format("- %s (%d unidades)", p.getName(), p.getQuantity()))
                .collect(Collectors.joining("\n"));

        // 4. Proactive Alerts Guidance
        StringBuilder alertsGuidance = new StringBuilder("== PRIORIDADES SUGERIDAS ==\n");
        if (stats.getPendingOrders() > 0) alertsGuidance.append("- Hay pedidos por procesar. Sugiere revisar [Pedidos](/admin/reservations).\n");
        if (stats.getLowStockCount() > 10) alertsGuidance.append("- El inventario esta bajo en varios items. Sugiere [Inventario](/admin/inventory).\n");
        if (stats.getSecurityAlertsCount() > 0) alertsGuidance.append("- Se han detectado alertas de seguridad. Sugiere revisar logs.\n");
        if (stats.getPendingSupportTicketsCount() > 0) alertsGuidance.append("- Clientes esperando respuesta. Sugiere [Soporte](/admin/support).\n");

        String storeName = settings.getStoreName() != null ? settings.getStoreName() : "BelMarket";
        return "Eres 'Mia Intelligence', la Asistente Ejecutiva y Analista de Negocios de " + storeName + ". Tu tono es analitico, estrategico y proactivo.\n\n" +
                financialCtx + "\n" +
                operationalCtx + "\n" +
                "== PRODUCTOS TOP VENTAS ==\n" + (topProductsCtx.isEmpty() ? "Sin datos de ventas aun." : topProductsCtx) + "\n\n" +
                alertsGuidance + "\n" +
                "INSTRUCCIONES DE ANALISTA:\n" +
                "1. ANALISIS PROACTIVO: No solo des datos. Si las ventas crecieron, felicita al admin. Si el crecimiento es negativo, sugiere revisar estrategias de marketing.\n" +
                "2. PRIORIZACION: Si hay alertas de seguridad o muchos pedidos pendientes, mencionarlos al inicio como prioridad.\n" +
                "3. ENLACES EJECUTIVOS: Usa siempre Markdown para dirigir al admin: [Ver Reportes](/admin/reports), [Gestionar Pedidos](/admin/reservations), [Inventario](/admin/inventory).\n" +
                "4. BREVEDAD: Se eficiente. Al admin le interesa el 'Bottom Line'.\n" +
                "5. EMOJIS: Usa emojis profesionales (📈, 📊, 🛡️, ⚙️).";
    }
}
