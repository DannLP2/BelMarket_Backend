package com.productos.mari.domain.category;

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
class CategoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();
    }

    @Test
    void getAll_ReturnsOk() throws Exception {
        when(categoryService.getAllCategoriesWithCount()).thenReturn(List.of(new CategoryDto()));
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
    }

    @Test
    void create_ReturnsOk() throws Exception {
        when(categoryService.createCategory(any(CategoryDto.class))).thenReturn(new CategoryDto());

        mockMvc.perform(post("/api/categories/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Category\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void update_ReturnsOk() throws Exception {
        when(categoryService.updateCategory(eq(1L), any(CategoryDto.class))).thenReturn(new CategoryDto());

        mockMvc.perform(put("/api/categories/admin/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated Category\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/categories/admin/1"))
                .andExpect(status().isNoContent());
        verify(categoryService).deleteCategory(1L);
    }
}
