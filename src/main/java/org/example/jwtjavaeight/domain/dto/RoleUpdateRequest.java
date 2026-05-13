package org.example.jwtjavaeight.domain.dto;

import org.example.jwtjavaeight.enums.DataScopeEnum;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.Size;

public class RoleUpdateRequest {
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

    // Getters and Setters
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
