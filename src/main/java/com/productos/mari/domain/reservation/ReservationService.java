package com.productos.mari.domain.reservation;

import com.productos.mari.domain.reservation.ReservationDto;
import com.productos.mari.domain.reservation.ReservationStatus;

import java.util.List;

public interface ReservationService {
    ReservationDto createReservation(ReservationDto reservationDto, String email);
    List<ReservationDto> getMyReservations(String email);
    List<ReservationDto> getAllReservations();
    ReservationDto updateReservationStatus(Long id, ReservationStatus status);
    ReservationDto updateReservation(Long id, ReservationDto reservationDto, String email);
    ReservationDto cancelReservation(Long id, String email);
    int deleteAllCancelled();
    byte[] getReservationPdf(Long id, String email, String targetCurrency);
    boolean hasUserPurchasedProduct(String email, Long productId);
    com.productos.mari.domain.reservation.ReservationItemDto getReservationItem(Long itemId, String email);
    
    // Deliverer methods
    List<ReservationDto> getAvailableOrders();
    List<ReservationDto> getAssignedOrders(String delivererEmail);
    ReservationDto assignToMe(Long reservationId, String delivererEmail);
    ReservationDto completeReservationWithProof(Long id, String code, org.springframework.web.multipart.MultipartFile image);
}
