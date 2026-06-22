package com.productos.mari.domain.brand;

import com.productos.mari.domain.brand.Brand;
import com.productos.mari.domain.brand.BrandDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    Optional<Brand> findByNameIgnoreCase(String name);

    @Query("SELECT new com.productos.mari.domain.brand.BrandDto(b.id, b.name, COUNT(p)) " +
           "FROM Brand b LEFT JOIN Product p ON p.brand = b " +
           "GROUP BY b.id, b.name ORDER BY b.name ASC")
    List<BrandDto> findAllWithProductCount();
}
