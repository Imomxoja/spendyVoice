package org.example.domain.response;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class BaseResponse<T> {
    private String message;
    private Integer status;
    private T data;
}
