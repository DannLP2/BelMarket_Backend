package com.productos.mari.domain.infrastructure.audit;

import com.productos.mari.domain.marketing.OfferDto;
import com.productos.mari.domain.infrastructure.audit.ProductAuditDto;
import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.reservation.ReservationItemRepository;
import com.productos.mari.domain.marketing.OfferService;
import com.productos.mari.domain.infrastructure.audit.ProductAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductAuditServiceImpl implements ProductAuditService {

    private final ProductRepository productRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final OfferService offerService;

    @Override
    @Transactional(readOnly = true)
    public List<ProductAuditDto> getProductAudits() {
        List<Product> products = productRepository.findAll();
        
        return products.stream().map(product -> {
            OfferDto activeOffer = offerService.getActiveOfferForProduct(product.getId());
            
            Object[] stats = reservationItemRepository.getProductSalesStats(product.getId());
            
            Integer totalUnitsSold = 0;
            BigDecimal totalRevenue = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;
            
            if (stats != null && stats.length > 0 && stats[0] != null) {
                // If the query returns a single array, stats represents [0]=units, [1]=revenue, [2]=cost
                // However, spring data jpa usually returns List<Object[]> for aggregated queries, 
                // but since we query for a single product, it should return a single Object[] if we use a single result query.
                // Wait! If the query is just SELECT A, B, C FROM ... it returns a single Object[] directly!
                Object[] resultArr;
                if (stats[0] instanceof Object[]) {
                    resultArr = (Object[]) stats[0];
                } else {
                    resultArr = stats;
                }
                
                if (resultArr.length >= 3) {
                    totalUnitsSold = ((Number) resultArr[0]).intValue();
                    totalRevenue = new BigDecimal(resultArr[1].toString());
                    totalCost = new BigDecimal(resultArr[2].toString());
                }
            }
            
            BigDecimal totalProfit = totalRevenue.subtract(totalCost);
            
            BigDecimal margin = BigDecimal.ZERO;
            if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                margin = totalProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            } else if (product.getPrice() != null && product.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                // Fallback to theoretical margin if no sales
                 BigDecimal currentCost = product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO;
                 BigDecimal currentProfit = product.getPrice().subtract(currentCost);
                 margin = currentProfit.divide(product.getPrice(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
            
            return ProductAuditDto.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                    .categoryName(product.getCategories() != null && !product.getCategories().isEmpty() 
                            ? product.getCategories().stream().map(com.productos.mari.domain.category.Category::getName).collect(Collectors.joining(", ")) : null)
                    .mainImageUrl(product.getMainImageUrl())
                    .stock(product.getStock())
                    .purchasePrice(product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO)
                    .currentPrice(product.getPrice())
                    .marginPercentage(margin)
                    .totalUnitsSold(totalUnitsSold)
                    .totalRevenue(totalRevenue)
                    .totalProfit(totalProfit)
                    .hasActiveOffer(activeOffer != null)
                    .activeOfferDiscount(activeOffer != null ? activeOffer.getDiscountValue() : null)
                    .activeOfferType(activeOffer != null ? activeOffer.getDiscountType().name() : null)
                    .createdAt(product.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }
}
