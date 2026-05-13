package org.example.jwtjavaeight.domain.dto;

import org.example.jwtjavaeight.enums.MenuTypeEnum;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class MenuCreateRequest {
    private Long parentId;

    @NotBlank
    @Size(max = 32)
    private String menuName;

    @NotBlank
    @Size(max = 32)
    @Pattern(regexp = "^[a-z][a-z0-9\\-]*$")
    private String menuCode;

    @NotNull
    private MenuTypeEnum menuType;

    @Size(max = 200)
    private String path;

    @Size(max = 200)
    private String component;

    @Pattern(regexp = "^[a-z]+:[a-z\\-]+$")
    private String perms;

    @Size(max = 50)
    private String icon;

    @Min(0)
    private Integer sortOrder;

    private Boolean visible;

    private Integer status;

    @Size(max = 255)
    private String remark;

    // Getters and Setters
    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getMenuCode() {
        return menuCode;
    }

    public void setMenuCode(String menuCode) {
        this.menuCode = menuCode;
    }

    public MenuTypeEnum getMenuType() {
        return menuType;
    }

    public void setMenuType(MenuTypeEnum menuType) {
        this.menuType = menuType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getPerms() {
        return perms;
    }

    public void setPerms(String perms) {
        this.perms = perms;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
