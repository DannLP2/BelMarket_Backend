package com.productos.mari.domain.reservation;
import com.productos.mari.domain.user.UserStatsDto;

import com.productos.mari.domain.reservation.Reservation;
import com.productos.mari.domain.reservation.ReservationStatus;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserOrderByCreatedAtDesc(User user);
    List<Reservation> findAllByOrderByCreatedAtDesc();
    List<Reservation> findByStatus(ReservationStatus status);
    long countByStatus(ReservationStatus status);
    List<Reservation> findByReferenceIsNull();
    void deleteByStatus(ReservationStatus status);

    @Query("SELECT COUNT(r) > 0 FROM Reservation r JOIN r.items i WHERE r.user = :user AND r.status = :status AND i.product = :product")
    boolean existsByUserAndStatusAndProductInItems(@Param("user") User user, @Param("status") ReservationStatus status, @Param("product") Product product);

    @Query("SELECT new com.productos.mari.domain.user.UserStatsDto(" +
           "COUNT(r), " +
           "SUM(r.total), " +
           "AVG(CAST(r.total as double)), " +
           "MAX(r.createdAt)) " +
           "FROM Reservation r " +
           "WHERE r.user.id = :userId AND r.status = com.productos.mari.domain.reservation.ReservationStatus.COMPLETED")
    com.productos.mari.domain.user.UserStatsDto getUserStats(@Param("userId") Long userId);

    @Query("SELECT SUM(r.total) FROM Reservation r WHERE r.status = com.productos.mari.domain.reservation.ReservationStatus.COMPLETED")
    java.math.BigDecimal calculateTotalRevenue();

    @Query("SELECT SUM((i.price - i.purchasePrice) * i.quantity) FROM Reservation r JOIN r.items i WHERE r.status = com.productos.mari.domain.reservation.ReservationStatus.COMPLETED")
    java.math.BigDecimal calculateTotalProfit();

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.status = com.productos.mari.domain.reservation.ReservationStatus.PENDING")
    Long countPendingReservations();
    
    @Query("SELECT r FROM Reservation r JOIN FETCH r.user WHERE r.createdAt >= :start AND r.createdAt <= :end AND r.status = com.productos.mari.domain.reservation.ReservationStatus.COMPLETED")
    List<Reservation> findCompletedBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    List<Reservation> findByStatusIn(List<ReservationStatus> statuses);
    List<Reservation> findByDelivererAndStatus(User deliverer, ReservationStatus status);
    List<Reservation> findByDelivererAndStatusIn(User deliverer, java.util.List<ReservationStatus> statuses);

    @Query("SELECT r.deliverer.id as id, r.deliverer.name as name, r.deliverer.email as email, " +
           "r.deliverer.profilePictureUrl as profilePictureUrl, " +
           "COUNT(r) as totalAssigned, " +
           "SUM(CASE WHEN r.status = com.productos.mari.domain.reservation.ReservationStatus.COMPLETED THEN 1 ELSE 0 END) as totalCompleted, " +
           "SUM(CASE WHEN r.status = com.productos.mari.domain.reservation.ReservationStatus.CANCELLED THEN 1 ELSE 0 END) as totalCancelled, " +
           "MAX(r.completedAt) as lastDeliveryDate, " +
           "MAX(r.deliveryImageUrl) as lastPhoto " +
           "FROM Reservation r " +
           "WHERE r.deliverer IS NOT NULL " +
           "GROUP BY r.deliverer.id, r.deliverer.name, r.deliverer.email, r.deliverer.profilePictureUrl")
    List<Object[]> findDelivererSummary();

    @Query("SELECT r.user.id as id, r.user.name as name, r.user.email as email, " +
           "r.user.profilePictureUrl as profilePictureUrl, " +
           "COUNT(r) as totalOrders, " +
           "SUM(CASE WHEN r.status = com.productos.mari.domain.reservation.ReservationStatus.COMPLETED THEN 1 ELSE 0 END) as completedOrders, " +
           "SUM(CASE WHEN r.status = com.productos.mari.domain.reservation.ReservationStatus.CANCELLED THEN 1 ELSE 0 END) as cancelledOrders, " +
           "SUM(CASE WHEN r.status = com.productos.mari.domain.reservation.ReservationStatus.COMPLETED THEN r.total ELSE 0 END) as totalSpent, " +
           "MAX(r.createdAt) as lastOrderDate, " +
           "r.user.lastActiveAt as lastActiveAt, " +
           "r.user.createdAt as createdAt " +
           "FROM Reservation r " +
           "GROUP BY r.user.id, r.user.name, r.user.email, r.user.profilePictureUrl, r.user.lastActiveAt, r.user.createdAt")
    List<Object[]> findUserAuditSummary();

    // ── Performance-optimized aggregate queries ────────────────────────────────

    /** Suma de ingresos de reservas COMPLETADAS en un rango de fechas (sin cargar entidades). */
    @Query("SELECT COALESCE(SUM(r.total), 0) FROM Reservation r " +
           "WHERE r.status = com.productos.mari.domain.reservation.ReservationStatus.COMPLETED " +
           "AND r.createdAt >= :start AND r.createdAt <= :end")
    Double sumRevenueBetween(@Param("start") java.time.LocalDateTime start,
                             @Param("end") java.time.LocalDateTime end);

    /** Conteo de reservas COMPLETADAS en un rango de fechas. */
    @Query("SELECT COUNT(r) FROM Reservation r " +
           "WHERE r.status = com.productos.mari.domain.reservation.ReservationStatus.COMPLETED " +
           "AND r.createdAt >= :start AND r.createdAt <= :end")
    Long countCompletedBetween(@Param("start") java.time.LocalDateTime start,
                               @Param("end") java.time.LocalDateTime end);

    /**
     * Reemplaza el bucle de 12 queries. Devuelve [mes(1-12), revenue]
     * solo para los meses con ventas; el servicio rellena los demás con 0.
     */
    @Query(value = "SELECT MONTH(r.created_at) AS month, COALESCE(SUM(r.total), 0) AS revenue " +
                   "FROM reservations r " +
                   "WHERE r.status = 'COMPLETED' AND YEAR(r.created_at) = :year " +
                   "GROUP BY MONTH(r.created_at) ORDER BY month",
           nativeQuery = true)
    List<Object[]> findMonthlyRevenueByYear(@Param("year") int year);

    /**
     * Reemplaza la carga de 100 reservas para el gráfico de 7 días.
     * Devuelve [fecha('YYYY-MM-DD'), revenue] agrupado por día.
     */
    @Query(value = "SELECT DATE(r.created_at) AS day, COALESCE(SUM(r.total), 0) AS revenue " +
                   "FROM reservations r " +
                   "WHERE r.status = 'COMPLETED' " +
                   "AND r.created_at >= :start AND r.created_at <= :end " +
                   "GROUP BY DATE(r.created_at)",
           nativeQuery = true)
    List<Object[]> findDailyRevenueBetween(@Param("start") java.time.LocalDateTime start,
                                           @Param("end") java.time.LocalDateTime end);
}
