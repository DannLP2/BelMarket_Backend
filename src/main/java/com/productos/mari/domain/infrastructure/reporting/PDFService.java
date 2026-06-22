package com.productos.mari.domain.infrastructure.reporting;

import com.productos.mari.domain.reservation.ReservationDto;

public interface PDFService {
    byte[] generateReservationReceipt(ReservationDto reservation);
}
