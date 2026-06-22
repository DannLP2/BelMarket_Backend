package com.productos.mari.domain.product;

import com.productos.mari.domain.infrastructure.reporting.PageResponse;
import com.productos.mari.domain.infrastructure.audit.ProductAuditDto;
import com.productos.mari.domain.product.ProductDto;
import com.productos.mari.domain.infrastructure.audit.ProductAuditService;
import com.productos.mari.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductAuditService productAuditService;

    @GetMapping("/admin/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductAuditDto>> getProductAudits() {
        return ResponseEntity.ok(productAuditService.getProductAudits());
    }

    @GetMapping("/public")
    public ResponseEntity<PageResponse<ProductDto>> getAllPublicProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(productService.getAllProducts(PageRequest.of(page, size)));
    }

    @GetMapping("/public/search")
    public ResponseEntity<PageResponse<ProductDto>> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Boolean isMecatronic,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "false") boolean onlyOffers,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(productService.searchFiltered(q, category, brand, isMecatronic, minPrice, maxPrice, minRating, onlyOffers, PageRequest.of(page, size)));
    }

    @GetMapping("/public/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }

    @GetMapping("/public/brands")
    public ResponseEntity<List<String>> getAllBrands() {
        return ResponseEntity.ok(productService.getAllBrands());
    }

    @GetMapping("/public/slug/{slug}")
    public ResponseEntity<ProductDto> getPublicProductBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getProductBySlug(slug));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ProductDto> getPublicProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/public/{id}/related")
    public ResponseEntity<List<ProductDto>> getRelatedProducts(@PathVariable Long id, @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(productService.getRelatedProducts(id, limit));
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping(value = "/admin", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> createProduct(
            @RequestPart("product") @Valid ProductDto productDto,
            @RequestPart("mainImage") MultipartFile mainImage,
            @RequestPart(value = "technicalManuals", required = false) List<MultipartFile> technicalManuals) {
        return ResponseEntity.ok(productService.createProduct(productDto, mainImage, technicalManuals));
    }

    @PutMapping(value = "/admin/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") @Valid ProductDto productDto,
            @RequestPart(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestPart(value = "gallery", required = false) List<MultipartFile> gallery,
            @RequestPart(value = "technicalManuals", required = false) List<MultipartFile> technicalManuals) {
        return ResponseEntity.ok(productService.updateProduct(id, productDto, mainImage, gallery, technicalManuals));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
