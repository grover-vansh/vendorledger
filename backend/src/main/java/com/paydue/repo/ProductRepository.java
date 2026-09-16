package com.paydue.repo;

import com.paydue.domain.Product;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "supplier")
    List<Product> findBySupplierIdOrderByIdAsc(Long supplierId);

    long countBySupplierId(Long supplierId);

    boolean existsBySupplierIdAndSkuCode(Long supplierId, String skuCode);
}
