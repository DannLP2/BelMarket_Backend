package com.productos.mari.domain.marketing;

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
class OfferControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OfferService offerService;

    @InjectMocks
    private OfferController offerController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(offerController).build();
    }

    @Test
    void getActiveOffers_ReturnsOk() throws Exception {
        when(offerService.getActiveOffers()).thenReturn(List.of(new OfferDto()));
        mockMvc.perform(get("/api/offers/public/active"))
                .andExpect(status().isOk());
    }

    @Test
    void getActiveOfferForProduct_ReturnsOk() throws Exception {
        when(offerService.getActiveOfferForProduct(1L)).thenReturn(new OfferDto());
        mockMvc.perform(get("/api/offers/public/product/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllOffers_ReturnsOk() throws Exception {
        when(offerService.getAllOffers()).thenReturn(List.of(new OfferDto()));
        mockMvc.perform(get("/api/offers"))
                .andExpect(status().isOk());
    }

    @Test
    void createOffer_ReturnsOk() throws Exception {
        when(offerService.createOffer(any(OfferDto.class))).thenReturn(new OfferDto());

        mockMvc.perform(post("/api/offers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\": 1, \"discountValue\": 10.0}"))
                .andExpect(status().isOk());
    }

    @Test
    void deactivateOffer_ReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/offers/1"))
                .andExpect(status().isNoContent());
        verify(offerService).deactivateOffer(1L);
    }

    @Test
    void getActiveOfferForProduct_WhenNull_ReturnsNoContent() throws Exception {
        when(offerService.getActiveOfferForProduct(99L)).thenReturn(null);
        mockMvc.perform(get("/api/offers/public/product/99"))
                .andExpect(status().isNoContent());
    }
}
