package org.example.repository;

import org.example.domain.entity.VoiceCommandEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VoiceCommandRepository extends JpaRepository<VoiceCommandEntity, UUID> {

}
