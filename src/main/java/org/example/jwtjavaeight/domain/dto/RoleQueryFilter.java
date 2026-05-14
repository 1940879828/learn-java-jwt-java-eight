package org.example.jwtjavaeight.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RoleQueryFilter extends PageRequest {
    // Getters and Setters
    private String keyword;
    private Integer level;
    private String dataScope;

}
