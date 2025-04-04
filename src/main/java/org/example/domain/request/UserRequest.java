package org.example.domain.request;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
    private String fullName;
    private String password;
    private String email;
}
