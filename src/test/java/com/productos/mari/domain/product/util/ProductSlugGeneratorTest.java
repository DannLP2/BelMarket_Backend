package com.productos.mari.domain.product.util;

import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSlugGeneratorTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductSlugGenerator slugGenerator;

    @Test
    void generateSlug_NormalizesText() {
        when(productRepository.findBySlug(anyString())).thenReturn(Optional.empty());

        assertEquals("cerveza-aguila-poker", slugGenerator.generateSlug("Cerveza Águila & Poker!!"));
        assertEquals("nino-feliz", slugGenerator.generateSlug("Niño Feliz"));
    }

    @Test
    void generateSlug_HandlesEmptyOrNull() {
        when(productRepository.findBySlug(anyString())).thenReturn(Optional.empty());

        assertTrue(slugGenerator.generateSlug(null).startsWith("producto-"));
        assertTrue(slugGenerator.generateSlug("!!!").startsWith("producto-"));
    }

    @Test
    void generateSlug_HandlesDuplicates() {
        // First call finds a duplicate, second call (with -1) finds no duplicate
        when(productRepository.findBySlug("test-slug"))
                .thenReturn(Optional.of(new Product()));
        when(productRepository.findBySlug("test-slug-1"))
                .thenReturn(Optional.empty());

        String result = slugGenerator.generateSlug("Test Slug");

        assertEquals("test-slug-1", result);
        verify(productRepository, times(2)).findBySlug(anyString());
    }
}
