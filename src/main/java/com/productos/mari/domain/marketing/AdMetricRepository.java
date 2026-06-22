package com.productos.mari.domain.marketing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdMetricRepository extends JpaRepository<AdMetric, Long> {
}
