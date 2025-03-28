package org.example.domain.entity.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.example.domain.entity.user.Permission.*;

@RequiredArgsConstructor
public enum Role {
    USER(Set.of(USER_READ, USER_CREATE, USER_UPDATE, USER_DELETE)),
    ADMIN(Set.of(ADMIN_CREATE, ADMIN_UPDATE, ADMIN_READ, ADMIN_DELETE));

    @Getter
    private final Set<Permission> permissions;

    public List<SimpleGrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = getPermissions()
                .stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
                .collect(Collectors.toList());

        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        return authorities;
    }
}
