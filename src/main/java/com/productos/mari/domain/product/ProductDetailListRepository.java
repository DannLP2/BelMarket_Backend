package com.productos.mari.domain.product;

import com.productos.mari.domain.product.ProductDetailList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ProductDetailListRepository extends JpaRepository<ProductDetailList, Long> {
    List<ProductDetailList> findByProductId(Long productId);

    @Modifying
    @Transactional
    void deleteByProductId(Long productId);
}
