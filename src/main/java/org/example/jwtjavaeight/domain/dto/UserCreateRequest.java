package org.example.jwtjavaeight.domain.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Setter
@Getter
public class UserCreateRequest {
    // Getters and Setters
    @NotBlank
    @Size(min = 3, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private String username;

    @NotBlank
    @Size(min = 6, max = 64)
    private String password;

    @Size(max = 20)
    private String nickName;

    @Email
    @NotBlank
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$")
    private String phone;

    @Size(max = 255)
    private String remark;

}
