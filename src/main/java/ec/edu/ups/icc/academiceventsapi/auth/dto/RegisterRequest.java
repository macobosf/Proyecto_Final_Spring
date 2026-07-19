package ec.edu.ups.icc.academiceventsapi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres.")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio.")
        @Size(max = 80, message = "El apellido no puede superar los 80 caracteres.")
        String lastName,

        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        @Size(max = 160, message = "El correo no puede superar los 160 caracteres.")
        String email,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
        String password
) {
}
