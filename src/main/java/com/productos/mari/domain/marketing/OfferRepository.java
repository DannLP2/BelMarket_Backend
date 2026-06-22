package com.productos.mari.domain.marketing;

import com.productos.mari.domain.marketing.Offer;
import com.productos.mari.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    /** Oferta activa de un producto considerando fecha actual */
    /** Oferta activa de un producto considerando fecha actual (Retorna lista para evitar NonUniqueResultException) */
    @Query("SELECT o FROM Offer o WHERE o.product = :product AND o.active = true " +
            "AND (o.startDate IS NULL OR o.startDate <= :now) " +
            "AND (o.endDate IS NULL OR o.endDate >= :now) " +
            "ORDER BY o.createdAt DESC")
    List<Offer> findCurrentActiveByProduct(@Param("product") Product product, @Param("now") LocalDateTime now);

    /** Oferta activa por productId directo considerando fecha actual */
    @Query("SELECT o FROM Offer o WHERE o.product.id = :productId AND o.active = true " +
            "AND o.product.isActive = true " +
            "AND (o.startDate IS NULL OR o.startDate <= :now) " +
            "AND (o.endDate IS NULL OR o.endDate >= :now) " +
            "ORDER BY o.createdAt DESC")
    List<Offer> findCurrentActiveByProductId(@Param("productId") Long productId, @Param("now") LocalDateTime now);

    /** Todas las ofertas marcadas como activas (sin filtrar por fecha) */
    List<Offer> findAllByProductAndActiveTrue(Product product);

    /** Listado completo para el admin */
    List<Offer> findAllByOrderByCreatedAtDesc();

    /** Todas las ofertas activas de forma global considerando fecha actual */
    @Query("SELECT o FROM Offer o WHERE o.active = true " +
            "AND o.product.isActive = true " +
            "AND (o.startDate IS NULL OR o.startDate <= :now) " +
            "AND (o.endDate IS NULL OR o.endDate >= :now)")
    List<Offer> findAllCurrentActive(@Param("now") LocalDateTime now);
}
