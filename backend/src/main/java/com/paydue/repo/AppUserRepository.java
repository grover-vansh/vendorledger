package com.paydue.repo;

import com.paydue.domain.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"supplier", "buyer"})
    Optional<AppUser> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"supplier", "buyer"})
    @Query("select u from AppUser u order by u.id")
    List<AppUser> findAllWithCompanies();
}
