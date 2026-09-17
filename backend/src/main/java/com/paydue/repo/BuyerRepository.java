package com.paydue.repo;

import com.paydue.domain.Buyer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BuyerRepository extends JpaRepository<Buyer, Long> {

    @EntityGraph(attributePaths = "supplier")
    List<Buyer> findBySupplierIdOrderByNameAsc(Long supplierId);

    Optional<Buyer> findByIdAndSupplierId(Long id, Long supplierId);

    @EntityGraph(attributePaths = "supplier")
    @Query("select b from Buyer b where b.id = :id")
    Optional<Buyer> findByIdWithSupplier(@Param("id") Long id);

    boolean existsBySupplierIdAndNameIgnoreCase(Long supplierId, String name);

    boolean existsBySupplierIdAndEmailIgnoreCase(Long supplierId, String email);

    boolean existsBySupplierIdAndGstinIgnoreCase(Long supplierId, String gstin);
}
