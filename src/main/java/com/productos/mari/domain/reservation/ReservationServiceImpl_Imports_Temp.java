package com.productos.mari.domain.reservation;

import com.productos.mari.domain.user.User;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.user.UserRepository;
import com.productos.mari.domain.product.ProductService;
import com.productos.mari.domain.reservation.util.ReservationValidator;
import com.productos.mari.domain.reservation.util.ReservationPriceCalculator;
import com.productos.mari.domain.reservation.util.ReservationNotificationDispatcher;
import com.productos.mari.domain.notification.NotificationCategory;
import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.settings.AppSettingsService;
import com.productos.mari.domain.infrastructure.reporting.PDFService;
import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import com.productos.mari.domain.infrastructure.audit.SecurityAuditService;
import com.productos.mari.domain.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
