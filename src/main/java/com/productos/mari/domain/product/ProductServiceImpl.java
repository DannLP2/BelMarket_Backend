package com.productos.mari.domain.product;
import com.productos.mari.domain.product.ProductRepository;
import com.productos.mari.domain.marketing.OfferService;
import com.productos.mari.domain.product.ProductService;
import com.productos.mari.domain.brand.BrandRepository;
import com.productos.mari.domain.brand.Brand;

import lombok.extern.slf4j.Slf4j;

import com.productos.mari.domain.marketing.OfferDto;
import com.productos.mari.domain.infrastructure.reporting.PageResponse;
import com.productos.mari.domain.product.ProductManualDto;
import com.productos.mari.domain.product.util.ProductMediaManager;
import com.productos.mari.domain.product.util.ProductSlugGenerator;
import com.productos.mari.domain.notification.NotificationCategory;
import com.productos.mari.domain.marketing.Offer;
import com.productos.mari.domain.product.Product;
import com.productos.mari.domain.product.ProductManual;
import com.productos.mari.domain.marketing.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final OfferRepository offerRepository;
    private final OfferService offerService;
    private final com.productos.mari.domain.category.CategoryRepository categoryRepository;
    private final com.productos.mari.domain.brand.BrandRepository brandRepository;
    private final com.productos.mari.domain.reservation.ReservationItemRepository reservationItemRepository;
    private final com.productos.mari.domain.notification.NotificationService notificationService;
    private final com.productos.mari.domain.infrastructure.audit.SecurityAuditService securityAuditService;
    private final ProductMapper productMapper;
    private final ProductMediaManager productMediaManager;
    private final ProductSlugGenerator productSlugGenerator;
    
    @Override
    public PageResponse<ProductDto> getAllProducts(Pageable pageable) {
        Page<Product> productsPage = productRepository.findByIsActiveTrue(pageable);
        return mapToPageResponse(productsPage);
    }

    @Override
    public PageResponse<ProductDto> searchProducts(String query, Pageable pageable) {
        Page<Product> productsPage = productRepository.findByQuery(query, pageable);
        return mapToPageResponse(productsPage);
    }

    @Override
    public PageResponse<ProductDto> searchFiltered(String query, String category, String brand, Boolean isMecatronic,
                                                  java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, 
                                                  Double minRating, Boolean onlyOffers, Pageable pageable) {
        // Handle empty strings as null for JPA query
        String q = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        String cat = (category != null && !category.trim().isEmpty()) ? category.trim() : null;
        String b = (brand != null && !brand.trim().isEmpty()) ? brand.trim() : null;
        boolean offersOnly = onlyOffers != null && onlyOffers;
        
        Page<Product> productsPage = productRepository.findFiltered(q, cat, b, isMecatronic, minPrice, maxPrice, minRating, offersOnly, java.time.LocalDateTime.now(), pageable);
        return mapToPageResponse(productsPage);
    }

    @Override
    public java.util.List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    @Override
    public java.util.List<String> getAllBrands() {
        return productRepository.findAllBrands();
    }

    private PageResponse<ProductDto> mapToPageResponse(Page<Product> page) {
        List<ProductDto> content = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PageResponse.<ProductDto>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    public ProductDto getProductById(Long id) {
        return productRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new com.productos.mari.exception.ResourceNotFoundException("Product not found"));
    }

    @Override
    public ProductDto getProductBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .map(this::mapToDto)
                .orElseThrow(() -> new com.productos.mari.exception.ResourceNotFoundException("Product not found with slug: " + slug));
    }

    @Override
    public List<ProductDto> getRelatedProducts(Long id, int limit) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new com.productos.mari.exception.ResourceNotFoundException("Product not found"));
        
        com.productos.mari.domain.category.Category category = (product.getCategories() != null && !product.getCategories().isEmpty()) 
                ? product.getCategories().iterator().next() : null;

        return productRepository.findRelatedProducts(
                id, category, product.getBrand(), org.springframework.data.domain.PageRequest.of(0, limit))
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductDto createProduct(ProductDto productDto, MultipartFile mainImage, List<MultipartFile> technicalManuals) {
        Product product = productMapper.toEntity(productDto);
        
        // 1. Technical Field Generation
        product.setSlug(productSlugGenerator.generateSlug(productDto.getName()));
        product.setBrand(getOrCreateBrand(productDto.getBrand()));
        product.setCategories(getOrCreateCategories(productDto.getCategories()));
        if (product.getIsMecatronic() == null) product.setIsMecatronic(false);
        if (product.getIsActive() == null) product.setIsActive(true);
        
        // 2. Media Management
        product.setMainImageUrl(productMediaManager.uploadMainImage(mainImage));
        product.setGalleryImageUrls(new java.util.ArrayList<>());
        product.setManuals(productMediaManager.uploadManuals(technicalManuals));

        if (productDto.getDetailLists() != null) {
            product.setDetailLists(productDto.getDetailLists().stream()
                    .map(listDto -> com.productos.mari.domain.product.ProductDetailList.builder()
                            .title(listDto.getTitle())
                            .displayType(listDto.getDisplayType() != null ? listDto.getDisplayType() : "GRID")
                            .items(new java.util.ArrayList<>(listDto.getItems()))
                            .product(product)
                            .build())
                    .collect(Collectors.toList()));
        }

        Product saved = productRepository.save(product);
        
        String currentAdmin = getCurrentAuditor();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.PRODUCT_CREATED,
            null,
            currentAdmin,
            "Producto creado: " + saved.getName() + " (ID: " + saved.getId() + ")"
        );

        notificationService.broadcastCatalogUpdate("PRODUCT_CREATED");

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ProductDto updateProduct(Long id, ProductDto productDto, MultipartFile mainImage, List<MultipartFile> gallery, List<MultipartFile> technicalManuals) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new com.productos.mari.exception.ResourceNotFoundException("Product not found"));

        boolean hasRev = reservationItemRepository.existsByProductId(id);

        // 1. Media handling
        productMediaManager.handleMainImageUpdate(product, mainImage, hasRev);
        product.setGalleryImageUrls(productMediaManager.updateGallery(product, gallery, productDto.getGalleryImageUrls(), hasRev));
        product.setManuals(productMediaManager.updateManuals(product, technicalManuals, productDto.getManuals(), hasRev));
        
        product.setVideoUrl(productDto.getVideoUrl());

        // 2. Metadata and Core Fields
        if (productDto.getName() != null && !productDto.getName().equals(product.getName())) {
            product.setSlug(productSlugGenerator.generateSlug(productDto.getName()));
        }
        
        productMapper.updateEntityFromDto(productDto, product);
        
        // Garantizar que no se sobrescriban con nulo si el DTO no los trae (Retrocompatibilidad)
        if (productDto.getIsActive() != null) product.setIsActive(productDto.getIsActive());
        if (productDto.getIsMecatronic() != null) product.setIsMecatronic(productDto.getIsMecatronic());
        product.setBrand(getOrCreateBrand(productDto.getBrand()));
        product.setCategories(getOrCreateCategories(productDto.getCategories()));
        if (productDto.getIsMecatronic() != null) {
            product.setIsMecatronic(productDto.getIsMecatronic());
        }

        if (productDto.getDetailLists() != null) {
            product.getDetailLists().clear();
            product.getDetailLists().addAll(productDto.getDetailLists().stream()
                    .map(listDto -> com.productos.mari.domain.product.ProductDetailList.builder()
                            .title(listDto.getTitle())
                            .displayType(listDto.getDisplayType() != null ? listDto.getDisplayType() : "GRID")
                            .items(new java.util.ArrayList<>(listDto.getItems()))
                            .product(product)
                            .build())
                    .collect(Collectors.toList()));
        }
        
        Product saved = productRepository.save(product);

        String currentAdmin = getCurrentAuditor();
        securityAuditService.log(
            com.productos.mari.domain.auth.SecurityAction.PRODUCT_UPDATED,
            null,
            currentAdmin,
            "Producto actualizado: " + saved.getName() + " (ID: " + saved.getId() + ")"
        );
        
        notificationService.broadcastCatalogUpdate("PRODUCT_UPDATED");
        
        return mapToDto(saved);
    }

    private java.util.Set<com.productos.mari.domain.category.Category> getOrCreateCategories(java.util.List<String> names) {
        if (names == null || names.isEmpty()) return new java.util.HashSet<>();
        return names.stream()
                .filter(name -> name != null && !name.trim().isEmpty())
                .map(name -> getOrCreateCategory(name))
                .collect(Collectors.toSet());
    }

    private com.productos.mari.domain.category.Category getOrCreateCategory(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String normalized = name.trim();
        return categoryRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> categoryRepository.save(com.productos.mari.domain.category.Category.builder().name(normalized).build()));
    }

    private com.productos.mari.domain.brand.Brand getOrCreateBrand(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String normalized = name.trim();
        return brandRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> brandRepository.save(com.productos.mari.domain.brand.Brand.builder().name(normalized).build()));
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        // Al usar Soft-Delete, NO borramos las imágenes de Cloudinary.
        // Esto permite que pedidos antiguos sigan mostrando la foto.
        productRepository.findById(id).ifPresent(p -> {
            String currentAdmin = getCurrentAuditor();
            securityAuditService.log(
                com.productos.mari.domain.auth.SecurityAction.PRODUCT_DELETED,
                null,
                currentAdmin,
                "Producto eliminado (soft-delete): " + p.getName() + " (ID: " + id + ")"
            );
        });
        productRepository.deleteById(id);
        notificationService.broadcastCatalogUpdate("PRODUCT_DELETED");
    }

    private void notifyAdmins(String title, String description, String icon, String link, NotificationCategory category) {
        notificationService.broadcastNotification(title, description, icon, link, category, true);
    }

    @org.springframework.transaction.annotation.Transactional
    public int incrementStock(Long id, int qty) {
        int result = productRepository.incrementStock(id, qty);
        if (result > 0) {
            String currentAdmin = getCurrentAuditor();
            productRepository.findById(id).ifPresent(p -> {
                securityAuditService.log(
                    com.productos.mari.domain.auth.SecurityAction.PRODUCT_STOCK_ADJUSTED,
                    null,
                    currentAdmin,
                    "Stock incrementado automáticamente/manualmente en " + qty + " unidades para: " + p.getName() + " (ID: " + id + ")"
                );
            });
        }
        return result;
    }

    private String getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "SYSTEM";
    }

    private ProductDto mapToDto(Product product) {
        ProductDto dto = productMapper.toDto(product);

        List<Offer> activeOffers = offerRepository.findCurrentActiveByProduct(product, java.time.LocalDateTime.now());
        if (!activeOffers.isEmpty()) {
            OfferDto activeOfferDto = offerService.mapToDto(activeOffers.get(0));
            dto.setActiveOffer(activeOfferDto);
            dto.setDiscountedPrice(activeOfferDto.getFinalPrice());
        }
        return dto;
    }

    @Override
    @Transactional
    public int decrementStock(Long id, int qty) {
        int result = productRepository.decrementStock(id, qty);
        if (result > 0) {
            productRepository.findById(id).ifPresent(product -> {
                if (product.getStock() != null && product.getStock() < 5) {
                    notifyAdmins(
                        "ALERTA DE STOCK BAJO: " + product.getName(),
                        "El producto solo tiene " + product.getStock() + " unidades disponibles. Favor reponer pronto.",
                        "warning",
                        "/admin/products?q=" + product.getName(),
                        NotificationCategory.WARNING
                    );
                }
            });
        }
        return result;
    }

}
