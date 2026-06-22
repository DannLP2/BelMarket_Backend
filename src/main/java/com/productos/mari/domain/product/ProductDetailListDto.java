package com.productos.mari.domain.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailListDto {
    private Long id;
    private String title;
    private String displayType;
    private List<String> items;
}
