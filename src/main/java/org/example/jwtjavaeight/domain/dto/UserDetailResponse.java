package org.example.jwtjavaeight.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class UserDetailResponse extends UserResponse {
    // Getters and Setters
    private List<RoleResponse> roles;
    private List<String> permissions;
    private List<MenuTreeNode> menuTree;

}
