package org.example.repository;

import org.example.domain.entity.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, UUID> {

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE dataset", nativeQuery = true)
    void truncate();

    @Query(value = "SELECT * FROM dataset WHERE product ILIKE CONCAT('%', :product) LIMIT 1", nativeQuery = true)
    Optional<Dataset> searchForCategoryByProductName(@Param("product") String product);
}
