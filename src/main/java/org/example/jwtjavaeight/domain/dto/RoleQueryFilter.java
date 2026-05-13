package org.example.jwtjavaeight.domain.dto;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RoleQueryFilter extends PageRequest {
    private String keyword;
    private Integer level;
    private String dataScope;

    @Override
    protected Set<String> allowedSortColumns() {
        return new HashSet<>(Arrays.asList("id", "role_code", "role_name", "level", "create_time"));
    }

    // Getters and Setters
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }
}
