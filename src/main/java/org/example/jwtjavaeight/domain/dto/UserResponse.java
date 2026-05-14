package org.example.jwtjavaeight.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
public class UserResponse {
    // Getters and Setters
    private Long id;
    private String username;
    private String email;
    private String phone;
    private Integer status;
    private Boolean locked;
    private String createBy;
    private OffsetDateTime createTime;
    private String remark;

}
