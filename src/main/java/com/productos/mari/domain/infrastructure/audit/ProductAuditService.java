package com.productos.mari.domain.infrastructure.audit;

import com.productos.mari.domain.infrastructure.audit.ProductAuditDto;
import java.util.List;

public interface ProductAuditService {
    List<ProductAuditDto> getProductAudits();
}
