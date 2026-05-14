package org.example.jwtjavaeight.domain.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.jwtjavaeight.enums.DataScopeEnum;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.Size;

@Setter
@Getter
public class RoleUpdateRequest {
    // Getters and Setters
    @Size(max = 32)
    private String roleName;

    @Size(max = 100)
    private String permission;

    @Min(0)
    @Max(9)
    private Integer level;

    private DataScopeEnum dataScope;

    @Size(max = 255)
    private String remark;

}
