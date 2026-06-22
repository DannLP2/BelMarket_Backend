package com.productos.mari.domain.product.util;

import com.productos.mari.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductSlugGenerator {

    private final ProductRepository productRepository;

    public String generateSlug(String name) {
        if (name == null) return "producto-" + System.currentTimeMillis();
        String slug = name.toLowerCase()
                .replaceAll("[áàäâ]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöô]", "o")
                .replaceAll("[úùüû]", "u")
                .replaceAll("[ñ]", "n")
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-", "")
                .replaceAll("-$", "");
        
        if (slug.isEmpty()) slug = "producto-" + System.currentTimeMillis();

        String finalSlug = slug;
        int count = 1;
        while (productRepository.findBySlug(finalSlug).isPresent()) {
            finalSlug = slug + "-" + count++;
        }
        return finalSlug;
    }
}
