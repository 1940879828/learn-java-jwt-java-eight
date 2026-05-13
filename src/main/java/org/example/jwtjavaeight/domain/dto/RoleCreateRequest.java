package org.example.jwtjavaeight.domain.dto;

import org.example.jwtjavaeight.enums.DataScopeEnum;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class RoleCreateRequest {
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

    // Getters and Setters
    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public DataScopeEnum getDataScope() {
        return dataScope;
    }

    public void setDataScope(DataScopeEnum dataScope) {
        this.dataScope = dataScope;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
