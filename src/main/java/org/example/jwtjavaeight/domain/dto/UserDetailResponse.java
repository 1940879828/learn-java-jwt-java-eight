package org.example.jwtjavaeight.domain.dto;

import java.util.List;

public class UserDetailResponse extends UserResponse {
    private List<RoleResponse> roles;
    private List<String> permissions;
    private List<MenuTreeNode> menuTree;

    // Getters and Setters
    public List<RoleResponse> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleResponse> roles) {
        this.roles = roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public List<MenuTreeNode> getMenuTree() {
        return menuTree;
    }

    public void setMenuTree(List<MenuTreeNode> menuTree) {
        this.menuTree = menuTree;
    }
}
