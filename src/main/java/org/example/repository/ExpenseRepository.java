package org.example.repository;

import org.example.domain.entity.expense.ExpenseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<ExpenseEntity, UUID> {
    @Query("select e from expenses e where e.user.id = :id")
    Page<ExpenseEntity> getAll(@Param("id") UUID id, Pageable pageable);
}
