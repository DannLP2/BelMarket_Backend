package com.productos.mari.domain.infrastructure.admin;

import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SeoControllerTest {

    private MockMvc mockMvc;

    @Mock private ProductRepository productRepository;

    @InjectMocks
    private SeoController seoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(seoController).build();
    }

    @Test
    void getProductSeoTags_ReturnsHtmlWithMetadata() throws Exception {
        Product product = Product.builder()
                .id(1L)
                .name("Test Product")
                .description("A very long description that should be truncated because it exceeds the limit of 150 characters to ensure we cover the substring branch in the controller logic.")
                .mainImageUrl("http://img.jpg")
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        mockMvc.perform(get("/api/public/seo/product/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<title>Test Product | BelMarket</title>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("og:image")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("...")));
    }

    @Test
    void getProductSeoTags_ReturnsNotFound_WhenIdInvalid() throws Exception {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/public/seo/product/99"))
                .andExpect(status().isNotFound());
    }
}
