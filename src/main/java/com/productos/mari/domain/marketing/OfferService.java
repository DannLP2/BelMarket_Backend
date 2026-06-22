package com.productos.mari.domain.marketing;

import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.marketing.OfferDto;

import java.util.List;

public interface OfferService {
    OfferDto createOffer(OfferDto offerDto);
    void deactivateOffer(Long id);
    List<OfferDto> getAllOffers();
    OfferDto getActiveOfferForProduct(Long productId);
    List<OfferDto> getActiveOffers();
    java.math.BigDecimal calculateFinalPriceForProduct(com.productos.mari.domain.product.Product product, Integer quantity);
    OfferDto mapToDto(com.productos.mari.domain.marketing.Offer offer);
}
