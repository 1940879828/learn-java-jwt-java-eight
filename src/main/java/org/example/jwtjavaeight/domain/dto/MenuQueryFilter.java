package org.example.jwtjavaeight.domain.dto;

import java.util.Set;

public class MenuQueryFilter extends PageRequest {
    private String keyword;
    private Integer menuType;
    private Integer visible;
    private Integer status;
    private Long parentId;

    @Override
    protected Set<String> allowedSortColumns() {
        return Set.of("id", "sort_order", "create_time");
    }

    // Getters and Setters
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getMenuType() {
        return menuType;
    }

    public void setMenuType(Integer menuType) {
        this.menuType = menuType;
    }

    public Integer getVisible() {
        return visible;
    }

    public void setVisible(Integer visible) {
        this.visible = visible;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
