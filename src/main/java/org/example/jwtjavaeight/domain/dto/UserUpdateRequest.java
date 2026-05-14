package org.example.jwtjavaeight.domain.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Setter
@Getter
public class UserUpdateRequest {
    // Getters and Setters
    @Email
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$")
    private String phone;

    private Integer status;

    @Size(max = 255)
    private String remark;

}
