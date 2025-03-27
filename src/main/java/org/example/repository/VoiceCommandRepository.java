package org.example.repository;

import org.example.domain.entity.VoiceCommandEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface VoiceCommandRepository extends JpaRepository<VoiceCommandEntity, UUID> {

    @Query("select c from commands c where c.user.id = :id")
    Page<VoiceCommandEntity> getAll(@Param("id") UUID userId, Pageable pageable);
}
