package org.example.jwtjavaeight.domain.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.jwtjavaeight.enums.DataScopeEnum;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Setter
@Getter
public class RoleCreateRequest {
    // Getters and Setters
    @NotBlank
    @Size(max = 32)
    @Pattern(regexp = "^[A-Z_]{2,32}$")
    private String roleCode;

    @NotBlank
    @Size(max = 32)
    private String roleName;

    @Size(max = 100)
    private String permission;

    @Min(0)
    @Max(9)
    private Integer level;

    @NotNull
    private DataScopeEnum dataScope;

    @Size(max = 255)
    private String remark;

}
