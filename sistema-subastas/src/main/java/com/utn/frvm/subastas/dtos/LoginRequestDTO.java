package com.utn.frvm.subastas.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequestDTO {

    @NotBlank(message = "{user.username.notblank}")
    private String username;

    @NotBlank(message = "{user.password.notblank}")
    private String password;
}
