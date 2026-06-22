package com.productos.mari.domain.brand;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BrandControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BrandService brandService;

    @InjectMocks
    private BrandController brandController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(brandController).build();
    }

    @Test
    void getAll_ReturnsOk() throws Exception {
        when(brandService.getAllBrandsWithCount()).thenReturn(List.of(new BrandDto()));
        mockMvc.perform(get("/api/brands"))
                .andExpect(status().isOk());
    }

    @Test
    void create_ReturnsOk() throws Exception {
        when(brandService.createBrand(any(BrandDto.class))).thenReturn(new BrandDto());

        mockMvc.perform(post("/api/brands/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Brand\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void update_ReturnsOk() throws Exception {
        when(brandService.updateBrand(eq(1L), any(BrandDto.class))).thenReturn(new BrandDto());

        mockMvc.perform(put("/api/brands/admin/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated Brand\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/brands/admin/1"))
                .andExpect(status().isNoContent());
        verify(brandService).deleteBrand(1L);
    }
}
