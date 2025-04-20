package org.example.domain.entity;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;

public class BaseEntityListener {
    @PrePersist
    public void prePersist(BaseEntity entity) {
        entity.createdBy = getAuthorizedName();
    }

    @PreUpdate
    public void preUpdate(BaseEntity entity) {
        entity.modifiedDate = LocalDateTime.now();
        entity.lastModifiedBy = getAuthorizedName();
    }
    public String getAuthorizedName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "not authenticated";
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }
}
