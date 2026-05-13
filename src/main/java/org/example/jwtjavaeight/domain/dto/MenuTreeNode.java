package org.example.jwtjavaeight.domain.dto;

import java.util.List;

public class MenuTreeNode extends MenuResponse {
    private List<MenuTreeNode> children;

    // Getters and Setters
    public List<MenuTreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<MenuTreeNode> children) {
        this.children = children;
    }
}
