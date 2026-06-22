package com.productos.mari.domain.marketing;

import com.productos.mari.domain.marketing.OfferDto;
import com.productos.mari.domain.marketing.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    /** Admin: crear oferta */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OfferDto> createOffer(@RequestBody OfferDto dto) {
        return ResponseEntity.ok(offerService.createOffer(dto));
    }

    /** Admin: desactivar oferta */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateOffer(@PathVariable Long id) {
        offerService.deactivateOffer(id);
        return ResponseEntity.noContent().build();
    }

    /** Admin: listar todas las ofertas */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OfferDto>> getAllOffers() {
        return ResponseEntity.ok(offerService.getAllOffers());
    }

    /** Public: oferta activa de un producto */
    @GetMapping("/public/product/{productId}")
    public ResponseEntity<OfferDto> getActiveOfferForProduct(@PathVariable Long productId) {
        OfferDto offer = offerService.getActiveOfferForProduct(productId);
        if (offer == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(offer);
    }

    /** Public: todas las ofertas activas globales */
    @GetMapping("/public/active")
    public ResponseEntity<List<OfferDto>> getActiveOffers() {
        return ResponseEntity.ok(offerService.getActiveOffers());
    }
}
