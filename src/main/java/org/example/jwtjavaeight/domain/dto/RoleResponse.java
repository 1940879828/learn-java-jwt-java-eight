package org.example.jwtjavaeight.domain.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.jwtjavaeight.enums.DataScopeEnum;

import java.time.OffsetDateTime;

@Setter
@Getter
public class RoleResponse {
    // Getters and Setters
    private Long id;
    private String roleCode;
    private String roleName;
    private String permission;
    private Integer level;
    private DataScopeEnum dataScope;
    private String createBy;
    private OffsetDateTime createTime;
    private String remark;

}
