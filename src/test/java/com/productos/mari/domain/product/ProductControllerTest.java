package com.productos.mari.domain.product;

import com.productos.mari.domain.infrastructure.audit.ProductAuditService;
import com.productos.mari.domain.infrastructure.reporting.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @Mock
    private ProductAuditService productAuditService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();
    }

    @Test
    void getAllPublicProducts_ReturnsOk() throws Exception {
        when(productService.getAllProducts(any())).thenReturn(new PageResponse<>());

        mockMvc.perform(get("/api/products/public"))
                .andExpect(status().isOk());
    }

    @Test
    void searchProducts_FiltersOk() throws Exception {
        when(productService.searchFiltered(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(new PageResponse<>());

        mockMvc.perform(get("/api/products/public/search")
                .param("q", "test")
                .param("onlyOffers", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void getPublicProductBySlug_ReturnsOk() throws Exception {
        when(productService.getProductBySlug("slug-1")).thenReturn(new ProductDto());

        mockMvc.perform(get("/api/products/public/slug/slug-1"))
                .andExpect(status().isOk());
    }

    @Test
    void createProduct_Success() throws Exception {
        String json = "{\"name\":\"Test\",\"brand\":\"Brand\",\"price\":1000,\"stock\":10}";
        MockMultipartFile productPart = new MockMultipartFile("product", "", "application/json", json.getBytes());
        MockMultipartFile imagePart = new MockMultipartFile("mainImage", "img.jpg", "image/jpeg", new byte[]{1, 2});

        when(productService.createProduct(any(), any(), any())).thenReturn(new ProductDto());

        mockMvc.perform(multipart("/api/products/admin")
                .file(productPart)
                .file(imagePart))
                .andExpect(status().isOk());
    }

    @Test
    void updateProduct_Success() throws Exception {
        String json = "{\"name\":\"Test\",\"brand\":\"Brand\",\"price\":1000,\"stock\":10}";
        MockMultipartFile productPart = new MockMultipartFile("product", "", "application/json", json.getBytes());
        MockMultipartFile imagePart = new MockMultipartFile("mainImage", "img.jpg", "image/jpeg", new byte[]{1, 2});

        when(productService.updateProduct(anyLong(), any(), any(), any(), any())).thenReturn(new ProductDto());

        mockMvc.perform(multipart("/api/products/admin/1")
                .file(productPart)
                .file(imagePart)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk());
    }

    @Test
    void deleteProduct_ReturnsNoContent() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/admin/1"))
                .andExpect(status().isNoContent());
    }
}
