package org.example.jwtjavaeight.domain.dto;

import java.util.Set;

public class UserQueryFilter extends PageRequest {
    private String keyword;
    private Integer status;
    private Boolean locked;
    private Long roleId;

    @Override
    protected Set<String> allowedSortColumns() {
        return Set.of("id", "username", "create_time");
    }

    // Getters and Setters
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
