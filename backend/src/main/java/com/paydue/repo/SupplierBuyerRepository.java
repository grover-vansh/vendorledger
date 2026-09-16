package com.paydue.repo;

import com.paydue.domain.SupplierBuyer;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierBuyerRepository extends JpaRepository<SupplierBuyer, Long> {

    boolean existsBySupplierIdAndBuyerId(Long supplierId, Long buyerId);

    @EntityGraph(attributePaths = "buyer")
    List<SupplierBuyer> findBySupplierId(Long supplierId);
}
