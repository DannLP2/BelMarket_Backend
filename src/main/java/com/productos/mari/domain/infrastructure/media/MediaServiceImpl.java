package com.productos.mari.domain.infrastructure.media;
import com.productos.mari.domain.user.Address;
import com.productos.mari.domain.settings.AppSettings;
import com.productos.mari.domain.marketing.Banner;
import com.productos.mari.domain.marketing.Advertiser;
import com.productos.mari.domain.reservation.Reservation;
import com.productos.mari.domain.reservation.ReservationItem;
import com.productos.mari.domain.support.SupportRequestRepository;

import com.productos.mari.domain.infrastructure.media.MediaDto;
import com.productos.mari.domain.infrastructure.media.MediaUsageDto;
import com.productos.mari.domain.settings.AppSettingsRepository;
import com.productos.mari.domain.user.AddressRepository;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.user.UserRepository;
import com.productos.mari.domain.infrastructure.media.CloudinaryService;
import com.productos.mari.domain.infrastructure.media.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.user.User;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final CloudinaryService cloudinaryService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AppSettingsRepository appSettingsRepository;
    private final com.productos.mari.domain.marketing.BannerRepository bannerRepository;
    private final com.productos.mari.domain.reservation.ReservationItemRepository reservationItemRepository;
    private final com.productos.mari.domain.reservation.ReservationRepository reservationRepository;
    private final com.productos.mari.domain.support.SupportRequestRepository supportRequestRepository;
    private final AddressRepository addressRepository;
    private final com.productos.mari.domain.marketing.AdvertiserRepository advertiserRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<MediaDto> getAllMedia() throws Exception {
        // 1. Obtener todos los recursos de Cloudinary
        List<MediaDto> allMedia = cloudinaryService.listResources();

        // 2. Cargar entidades para saber exactamente dónde se usa
        List<Product> products = productRepository.findAll();
        List<User> users = userRepository.findAll();
        List<AppSettings> settingsList = appSettingsRepository.findAll();
        List<Banner> banners = bannerRepository.findAll();
        List<Reservation> reservations = reservationRepository.findAll();
        List<ReservationItem> reservationItems = reservationItemRepository.findAll();
        List<Address> addresses = addressRepository.findAll();
        List<com.productos.mari.domain.support.SupportRequest> supportRequests = supportRequestRepository.findAll();
        List<Advertiser> advertisers = advertiserRepository.findAll();

        // 3. Evaluar cada media
        for (MediaDto media : allMedia) {
            String publicId = media.getPublicId();
            List<MediaUsageDto> usedIn = new ArrayList<>();

            for(Product p : products) {
                if (p.getMainImageUrl() != null && p.getMainImageUrl().contains(publicId)) {
                    usedIn.add(new MediaUsageDto("Producto", "Principal", p.getName(), "/admin/products"));
                }
                if (p.getGalleryImageUrls() != null && p.getGalleryImageUrls().stream().anyMatch(url -> url != null && url.contains(publicId))) {
                    usedIn.add(new MediaUsageDto("Producto", "Galería", p.getName(), "/admin/products"));
                }
                if (p.getManuals() != null && p.getManuals().stream().anyMatch(m -> m.getUrl() != null && m.getUrl().contains(publicId))) {
                    usedIn.add(new MediaUsageDto("Producto", "Manual Técnico", p.getName(), "/admin/products"));
                }
            }

            for(User u : users) {
                if(u.getProfilePictureUrl() != null && u.getProfilePictureUrl().contains(publicId)) {
                    usedIn.add(new MediaUsageDto("Usuario", "Avatar", u.getName(), "/admin/users"));
                }
            }

            for(Banner b : banners) {
                if(b.getImageUrl() != null && b.getImageUrl().contains(publicId)) {
                    usedIn.add(new MediaUsageDto("Banner", "Principal", b.getTitle(), "/admin/banners"));
                }
            }

            for(Advertiser adv : advertisers) {
                if(adv.getLogoUrl() != null && adv.getLogoUrl().contains(publicId)) {
                    usedIn.add(new MediaUsageDto("Anunciante", "Logo", adv.getName(), "/admin/advertisers"));
                }
                if(adv.getAdImageUrl() != null && adv.getAdImageUrl().contains(publicId)) {
                    usedIn.add(new MediaUsageDto("Anunciante", "Publicidad", adv.getAdTitle() != null ? adv.getAdTitle() : adv.getName(), "/admin/advertisers"));
                }
            }

            for(ReservationItem r : reservationItems) {
                if(r.getProductImageSnapshot() != null && r.getProductImageSnapshot().contains(publicId)) {
                    usedIn.add(new MediaUsageDto("Reserva", "Historial", "Snapshot: " + r.getProductNameSnapshot(), "/admin/reservations"));
                    break; // Just one reference is enough to prevent orphan status, avoiding huge lists for popular products
                }
            }

            for(Reservation res : reservations) {
                if(res.getDeliveryImageUrl() != null && res.getDeliveryImageUrl().contains(publicId)) {
                    usedIn.add(new MediaUsageDto("Reserva", "Evidencia", "Referencia: " + res.getReference(), "/admin/reservations"));
                    break;
                }
            }

            for(AppSettings s : settingsList) {
                if(s.getLogoUrl() != null && s.getLogoUrl().contains(publicId)) { usedIn.add(new MediaUsageDto("Sistema", "Ajustes", "Logo Tienda", "/admin/settings")); }
                if(s.getFaviconUrl() != null && s.getFaviconUrl().contains(publicId)) { usedIn.add(new MediaUsageDto("Sistema", "Ajustes", "Favicon", "/admin/settings")); }
                if(s.getBgLightUrl() != null && s.getBgLightUrl().contains(publicId)) { usedIn.add(new MediaUsageDto("Sistema", "Ajustes", "Fondo Día", "/admin/settings")); }
                if(s.getBgDarkUrl() != null && s.getBgDarkUrl().contains(publicId)) { usedIn.add(new MediaUsageDto("Sistema", "Ajustes", "Fondo Noche", "/admin/settings")); }
            }

            for(Address a : addresses) {
                if(a.getImageUrl() != null && a.getImageUrl().contains(publicId)) {
                    usedIn.add(new MediaUsageDto("Ubicación", a.getTitle(), "Ref: " + (a.getReference() != null ? a.getReference() : "Sin referencia"), "/admin/users"));
                }
            }

            for(com.productos.mari.domain.support.SupportRequest s : supportRequests) {
                if(s.getAttachmentUrl() != null && s.getAttachmentUrl().contains(publicId)) {
                    usedIn.add(new MediaUsageDto("Soporte", s.getRequestType(), "Adjunto de: " + s.getName(), "/admin/support"));
                    break;
                }
            }

            media.setUsedIn(usedIn);
            media.setInUse(!usedIn.isEmpty());
        }

        return allMedia;
    }

    @Override
    public void deleteMedia(String publicId) throws IOException {
        cloudinaryService.deleteFile(publicId);
    }

    @Override
    public void deleteMediaBulk(List<String> publicIds) throws IOException {
        for (String publicId : publicIds) {
            cloudinaryService.deleteFile(publicId);
        }
    }
}
