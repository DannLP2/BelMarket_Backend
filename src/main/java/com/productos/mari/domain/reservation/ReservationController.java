package com.productos.mari.domain.reservation;

import com.productos.mari.domain.reservation.ReservationDto;
import com.productos.mari.domain.reservation.ReservationStatus;
import com.productos.mari.domain.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final com.productos.mari.domain.infrastructure.admin.AdminReportingService adminReportingService;

    @PostMapping
    public ResponseEntity<ReservationDto> createReservation(
            @RequestBody ReservationDto reservationDto,
            @AuthenticationPrincipal UserDetails userDetails 
    ) {
        return ResponseEntity.ok(reservationService.createReservation(reservationDto, userDetails.getUsername()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReservationDto>> getMyReservations(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reservationService.getMyReservations(userDetails.getUsername()));
    }

    @GetMapping(value = "/{id}/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> getReservationPdf(
            @PathVariable Long id, 
            @RequestParam(required = false) String currency,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reservationService.getReservationPdf(id, userDetails.getUsername(), currency));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservationDto> updateReservation(
            @PathVariable Long id,
            @RequestBody ReservationDto reservationDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reservationService.updateReservation(id, reservationDto, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ReservationDto> cancelReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reservationService.cancelReservation(id, userDetails.getUsername()));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservationDto>> getAllReservations() {
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationDto> updateStatus(
            @PathVariable Long id,
            @RequestParam ReservationStatus status
    ) {
        return ResponseEntity.ok(reservationService.updateReservationStatus(id, status));
    }

    @DeleteMapping("/admin/cancelled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer> deleteAllCancelled() {
        int count = reservationService.deleteAllCancelled();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.productos.mari.domain.infrastructure.admin.AdminStatsDto> getAdminStats(@RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(adminReportingService.getAdminStats());
    }
    @GetMapping("/check-purchase/{productId}")
    public ResponseEntity<Boolean> checkUserPurchase(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) return ResponseEntity.ok(false);
        return ResponseEntity.ok(reservationService.hasUserPurchasedProduct(userDetails.getUsername(), productId));
    }

    @GetMapping("/item/{id}")
    public ResponseEntity<com.productos.mari.domain.reservation.ReservationItemDto> getReservationItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reservationService.getReservationItem(id, userDetails.getUsername()));
    }

    // Deliverer endpoints
    @GetMapping("/delivery/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERER')")
    public ResponseEntity<List<ReservationDto>> getAvailableOrders() {
        return ResponseEntity.ok(reservationService.getAvailableOrders());
    }

    @GetMapping("/delivery/assigned")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERER')")
    public ResponseEntity<List<ReservationDto>> getAssignedOrders(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reservationService.getAssignedOrders(userDetails.getUsername()));
    }

    @PatchMapping("/delivery/{id}/claim")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERER')")
    public ResponseEntity<ReservationDto> claimOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reservationService.assignToMe(id, userDetails.getUsername()));
    }

    @PatchMapping("/delivery/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERER')")
    public ResponseEntity<ReservationDto> updateDeliveryStatus(
            @PathVariable Long id,
            @RequestParam ReservationStatus status
    ) {
        // Admin can update any, Deliverer can update status (usually to COMPLETED)
        return ResponseEntity.ok(reservationService.updateReservationStatus(id, status));
    }

    @PostMapping("/delivery/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERER')")
    public ResponseEntity<ReservationDto> completeDelivery(
            @PathVariable Long id,
            @RequestParam String code,
            @RequestPart(value = "image", required = false) org.springframework.web.multipart.MultipartFile image
    ) {
        return ResponseEntity.ok(reservationService.completeReservationWithProof(id, code, image));
    }
}
