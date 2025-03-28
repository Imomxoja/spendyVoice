package org.example.domain.entity.user;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;
import org.example.domain.entity.BaseEntity;
import org.example.domain.entity.VoiceCommandEntity;
import org.example.domain.entity.expense.ExpenseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Entity(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity extends BaseEntity implements UserDetails {
    private String fullName;
    private String password;
    private String email;
    private Role role;
    @OneToMany(mappedBy = "user")
    private List<ExpenseEntity> expenses;
    @OneToMany(mappedBy = "user")
    private List<VoiceCommandEntity> commands;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.getAuthorities();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
