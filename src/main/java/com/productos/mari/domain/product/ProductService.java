package com.productos.mari.domain.product;

import com.productos.mari.domain.infrastructure.reporting.PageResponse;
import com.productos.mari.domain.product.ProductDto;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    PageResponse<ProductDto> getAllProducts(Pageable pageable);
    PageResponse<ProductDto> searchProducts(String query, Pageable pageable);
    PageResponse<ProductDto> searchFiltered(String query, String category, String brand, Boolean isMecatronic,
                                          java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, 
                                          Double minRating, Boolean onlyOffers, Pageable pageable);
    java.util.List<String> getAllCategories();
    java.util.List<String> getAllBrands();
    ProductDto getProductById(Long id);
    ProductDto createProduct(ProductDto productDto, MultipartFile mainImage, List<MultipartFile> technicalManuals);
    ProductDto updateProduct(Long id, ProductDto productDto, MultipartFile mainImage, List<MultipartFile> gallery, List<MultipartFile> technicalManuals);
    void deleteProduct(Long id);
    ProductDto getProductBySlug(String slug);
    List<ProductDto> getRelatedProducts(Long id, int limit);
    int decrementStock(Long id, int qty);
    int incrementStock(Long id, int qty);
}
