package org.example.jwtjavaeight.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MenuQueryFilter extends PageRequest {
    // Getters and Setters
    private String keyword;
    private Integer menuType;
    private Integer visible;
    private Integer status;
    private Integer parentId;

}
