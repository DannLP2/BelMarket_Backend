package com.productos.mari.domain.product;

import com.productos.mari.domain.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    java.util.Optional<Product> findBySlug(String slug);
    @Query("SELECT p FROM Product p WHERE p.isActive = true " +
            "ORDER BY CASE WHEN p.stock > 0 THEN 1 ELSE 0 END DESC, " +
            "(SELECT COUNT(o) FROM Offer o WHERE o.product = p AND o.active = true AND (o.startDate IS NULL OR o.startDate <= CURRENT_TIMESTAMP) AND (o.endDate IS NULL OR o.endDate >= CURRENT_TIMESTAMP)) DESC, " +
            "p.averageRating DESC, p.createdAt DESC")
    Page<Product> findByIsActiveTrue(Pageable pageable);

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :qty WHERE p.id = :id AND p.stock >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :qty WHERE p.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") int qty);
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE " +
            "p.isActive = true AND " +
            "(:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.brand.name) LIKE LOWER(CONCAT('%', :q, '%'))) AND " +
            "(:category IS NULL OR EXISTS (SELECT cat FROM p.categories cat WHERE cat.name = :category)) AND " +
            "(:brand IS NULL OR p.brand.name = :brand) AND " +
            "(:isMecatronic IS NULL OR p.isMecatronic = :isMecatronic) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:minRating IS NULL OR p.averageRating >= :minRating) " +
            "AND (:onlyOffers IS FALSE OR EXISTS (SELECT o FROM Offer o WHERE o.product = p AND o.active = true " +
            "AND (o.startDate IS NULL OR o.startDate <= :now) " +
            "AND (o.endDate IS NULL OR o.endDate >= :now))) " +
            "ORDER BY CASE WHEN p.stock > 0 THEN 1 ELSE 0 END DESC, " +
            "(SELECT COUNT(o) FROM Offer o WHERE o.product = p AND o.active = true AND (o.startDate IS NULL OR o.startDate <= :now) AND (o.endDate IS NULL OR o.endDate >= :now)) DESC, " +
            "p.averageRating DESC, p.createdAt DESC")
    Page<Product> findFiltered(
            @org.springframework.data.repository.query.Param("q") String q,
            @org.springframework.data.repository.query.Param("category") String category,
            @org.springframework.data.repository.query.Param("brand") String brand,
            @org.springframework.data.repository.query.Param("isMecatronic") Boolean isMecatronic,
            @org.springframework.data.repository.query.Param("minPrice") java.math.BigDecimal minPrice,
            @org.springframework.data.repository.query.Param("maxPrice") java.math.BigDecimal maxPrice,
            @org.springframework.data.repository.query.Param("minRating") Double minRating,
            @org.springframework.data.repository.query.Param("onlyOffers") boolean onlyOffers,
            @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE " +
            "p.isActive = true AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.brand.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> findByQuery(String query, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT cat.name FROM Product p JOIN p.categories cat WHERE p.isActive = true")
    java.util.List<String> findAllCategories();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.brand.name FROM Product p WHERE p.isActive = true AND p.brand IS NOT NULL")
    java.util.List<String> findAllBrands();

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE p.isActive = true AND p.id != :id AND " +
            "((:category IS NOT NULL AND :category MEMBER OF p.categories) OR " +
            "(:brand IS NOT NULL AND p.brand = :brand))")
    java.util.List<Product> findRelatedProducts(
            @org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("category") com.productos.mari.domain.category.Category category,
            @org.springframework.data.repository.query.Param("brand") com.productos.mari.domain.brand.Brand brand,
            Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p JOIN p.categories c WHERE c = :category")
    long countByCategory(@Param("category") com.productos.mari.domain.category.Category category);
    long countByBrand(com.productos.mari.domain.brand.Brand brand);

    @Query("SELECT p.mainImageUrl FROM Product p WHERE p.mainImageUrl IS NOT NULL")
    java.util.List<String> findAllMainImageUrls();

    @Query("SELECT img FROM Product p JOIN p.galleryImageUrls img")
    java.util.List<String> findAllGalleryImageUrls();

    @Query("SELECT SUM(p.purchasePrice * p.stock) FROM Product p")
    java.math.BigDecimal calculateTotalInvestment();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true AND p.stock < :threshold")
    Long countLowStockProducts(@Param("threshold") int threshold);

    @Query("SELECT DISTINCT p FROM Product p " +
           "JOIN ReservationItem ri ON ri.product = p " +
           "WHERE p.isMecatronic = true " +
           "AND ri.reservation.user.id = :userId " +
           "AND ri.reservation.status IN (com.productos.mari.domain.reservation.ReservationStatus.CONFIRMED, com.productos.mari.domain.reservation.ReservationStatus.COMPLETED)")
    java.util.List<Product> findPurchasedMecatronicProducts(@Param("userId") Long userId);

    // ── DevStats helpers
    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true")
    long countActiveProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.isMecatronic = true")
    long countMecatronicProducts();

    @Query("SELECT p.id, p.name, p.brand.name, p.mainImageUrl, p.stock, p.purchasePrice, p.price, " +
           "SUM(CASE WHEN r.status = com.productos.mari.domain.reservation.ReservationStatus.COMPLETED THEN ri.quantity ELSE 0 END), " +
           "SUM(CASE WHEN r.status = com.productos.mari.domain.reservation.ReservationStatus.COMPLETED THEN (ri.quantity * ri.price) ELSE 0 END), " +
           "SUM(CASE WHEN r.status = com.productos.mari.domain.reservation.ReservationStatus.COMPLETED THEN (ri.quantity * (ri.price - ri.purchasePrice)) ELSE 0 END), " +
           "p.createdAt " +
           "FROM Product p " +
           "LEFT JOIN ReservationItem ri ON ri.product = p " +
           "LEFT JOIN ri.reservation r " +
           "GROUP BY p.id, p.name, p.brand.name, p.mainImageUrl, p.stock, p.purchasePrice, p.price, p.createdAt")
    java.util.List<Object[]> findProductAuditSummary();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM products", nativeQuery = true)
    void hardDeleteAllProducts();
}
