package com.productos.mari.domain.reservation;

import com.productos.mari.domain.reservation.ReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;


public interface ReservationItemRepository extends JpaRepository<ReservationItem, Long> {

       boolean existsByProductId(Long productId);

       @Query("SELECT COALESCE(SUM(ri.quantity), 0) FROM ReservationItem ri " +
                     "WHERE ri.product.id = :productId " +
                     "AND ri.reservation.status IN ('CONFIRMED', 'COMPLETED') " +
                     "AND ri.reservation.createdAt >= :startDate " +
                     "AND (:endDate IS NULL OR ri.reservation.createdAt <= :endDate)")
       Integer sumQuantitySold(@Param("productId") Long productId,
                     @Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       @Query("SELECT COALESCE(SUM(ri.quantity), 0), " +
                     "COALESCE(SUM(ri.quantity * ri.price), 0), " +
                     "COALESCE(SUM(ri.quantity * COALESCE(ri.purchasePrice, 0)), 0) " +
                     "FROM ReservationItem ri " +
                     "WHERE ri.product.id = :productId " +
                     "AND ri.reservation.status IN ('CONFIRMED', 'COMPLETED')")
       Object[] getProductSalesStats(@Param("productId") Long productId);

    /**
     * Top productos más vendidos por unidades, en UNA query de agregación.
     * Usa productNameSnapshot para evitar JOIN a la tabla products.
     * Devuelve [nombre, totalUnidades, imagenSnapshot].
     */
    @Query(value = "SELECT ri.product_name_snapshot AS name, " +
                   "SUM(ri.quantity) AS qty, " +
                   "MAX(ri.product_image_snapshot) AS img " +
                   "FROM reservation_items ri " +
                   "JOIN reservations r ON ri.reservation_id = r.id " +
                   "WHERE r.status = 'COMPLETED' " +
                   "AND ri.product_name_snapshot IS NOT NULL " +
                   "GROUP BY ri.product_name_snapshot " +
                   "ORDER BY qty DESC",
           nativeQuery = true)
    List<Object[]> findTopProductsByQuantity(org.springframework.data.domain.Pageable pageable);
}
