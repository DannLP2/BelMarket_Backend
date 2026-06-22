package com.productos.mari.domain.user;
import com.productos.mari.domain.reservation.ReservationDto;

import com.productos.mari.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {
    private User user;
    private UserStatsDto stats;
    private List<ReservationDto> recentReservations;
}
