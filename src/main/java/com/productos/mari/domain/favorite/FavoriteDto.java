package com.productos.mari.domain.favorite;

import com.productos.mari.domain.product.ProductDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteDto {
    private Long id;
    private Long productId;
    private ProductDto product;
    private LocalDateTime createdAt;
}
