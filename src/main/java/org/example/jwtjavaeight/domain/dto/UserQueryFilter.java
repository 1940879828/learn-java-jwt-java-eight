package org.example.jwtjavaeight.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserQueryFilter extends PageRequest {
    // Getters and Setters
    private String keyword;
    private Integer status;
    private Boolean locked;
    private Long roleId;

}
