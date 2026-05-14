package org.example.jwtjavaeight.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class MenuTreeNode extends MenuResponse {
    // Getters and Setters
    private List<MenuTreeNode> children;

}
