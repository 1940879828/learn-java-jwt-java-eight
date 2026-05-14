package org.example.jwtjavaeight.domain.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.jwtjavaeight.enums.MenuTypeEnum;

import java.time.OffsetDateTime;

@Setter
@Getter
public class MenuResponse {
    // Getters and Setters
    private Long id;
    private Integer parentId;
    private String menuName;
    private String menuCode;
    private MenuTypeEnum menuType;
    private String path;
    private String component;
    private String perms;
    private String icon;
    private Integer sortOrder;
    private Boolean visible;
    private Integer status;
    private String createBy;
    private OffsetDateTime createTime;
    private String remark;

}
