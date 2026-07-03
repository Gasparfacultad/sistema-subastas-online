package com.utn.frvm.subastas.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequestDTO {

    @NotBlank(message = "{user.username.notblank}")
    @Size(min = 3, max = 50, message = "{user.username.size}")
    private String username;

    @NotBlank(message = "{user.email.notblank}")
    @Email(message = "{user.email.invalid}")
    @Size(max = 100, message = "{user.email.size}")
    private String email;

    @NotBlank(message = "{user.password.notblank}")
    @Size(min = 6, max = 100, message = "{user.password.size}")
    private String password;

    @NotBlank(message = "{user.rolSolicitado.notblank}")
    private String rolSolicitado;
}
