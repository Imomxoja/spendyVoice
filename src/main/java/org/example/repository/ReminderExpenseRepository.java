package org.example.repository;

import org.example.domain.entity.ReminderExpenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReminderExpenseRepository extends JpaRepository<ReminderExpenseEntity, UUID> {
    @Query("select r from reminders r " +
            "where r.user.id = :id and r.markedAsDone = false")
    List<ReminderExpenseEntity> getUserReminders(@Param("id") UUID id);

    @Query("select r from reminders r " +
            "where r.user.id = :id and r.due_date > now()" +
            "order by r.due_date asc")
    List<ReminderExpenseEntity> getScheduledReminders(@Param("id") UUID userId);
}
