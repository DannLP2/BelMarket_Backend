package com.productos.mari.domain.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.productos.mari.domain.infrastructure.audit.ProductAuditService;
import com.productos.mari.domain.infrastructure.reporting.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerAdminTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock private ProductService productService;
    @Mock private ProductAuditService productAuditService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();
    }

    @Test
    void getAllPublicProducts_Success() throws Exception {
        PageResponse<ProductDto> page = PageResponse.<ProductDto>builder().content(List.of()).build();
        when(productService.getAllProducts(any())).thenReturn(page);

        mockMvc.perform(get("/api/products/public"))
                .andExpect(status().isOk());
    }

    @Test
    void searchProducts_Success() throws Exception {
        PageResponse<ProductDto> page = PageResponse.<ProductDto>builder().content(List.of()).build();
        when(productService.searchFiltered(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/products/public/search")
                .param("q", "test"))
                .andExpect(status().isOk());
    }

    @Test
    void getProductById_Admin_Success() throws Exception {
        when(productService.getProductById(1L)).thenReturn(new ProductDto());

        mockMvc.perform(get("/api/products/admin/1"))
                .andExpect(status().isOk());
    }

    @Test
    void createProduct_Success() throws Exception {
        ProductDto dto = new ProductDto();
        dto.setName("New Product");
        dto.setBrand("Test Brand");
        dto.setPrice(new BigDecimal("100.00"));
        dto.setStock(10);
        
        MockMultipartFile productPart = new MockMultipartFile("product", "", "application/json", objectMapper.writeValueAsBytes(dto));
        MockMultipartFile mainImage = new MockMultipartFile("mainImage", "test.jpg", "image/jpeg", "content".getBytes());

        when(productService.createProduct(any(), any(), any())).thenReturn(dto);

        mockMvc.perform(multipart("/api/products/admin")
                .file(productPart)
                .file(mainImage))
                .andExpect(status().isOk());
    }

    @Test
    void updateProduct_Success() throws Exception {
        ProductDto dto = new ProductDto();
        dto.setName("Updated Product");
        dto.setBrand("Test Brand");
        dto.setPrice(new BigDecimal("100.00"));
        dto.setStock(10);
        
        MockMultipartFile productPart = new MockMultipartFile("product", "", "application/json", objectMapper.writeValueAsBytes(dto));
        
        when(productService.updateProduct(eq(1L), any(), any(), any(), any())).thenReturn(dto);

        mockMvc.perform(multipart("/api/products/admin/1")
                .file(productPart)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isOk());
    }

    @Test
    void deleteProduct_Success() throws Exception {
        mockMvc.perform(delete("/api/products/admin/1"))
                .andExpect(status().isNoContent());
        
        verify(productService).deleteProduct(1L);
    }
}
