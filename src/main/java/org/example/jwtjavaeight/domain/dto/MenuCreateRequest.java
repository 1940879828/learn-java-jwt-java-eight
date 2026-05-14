package org.example.jwtjavaeight.domain.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.jwtjavaeight.enums.MenuTypeEnum;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Setter
@Getter
public class MenuCreateRequest {
    // Getters and Setters
    private Integer parentId;

    @NotBlank
    @Size(max = 32)
    private String menuName;

    @NotBlank
    @Size(max = 32)
    @Pattern(regexp = "^[a-z][a-z0-9\\-]*$")
    private String menuCode;

    @NotNull
    private MenuTypeEnum menuType;

    @Size(max = 200)
    private String path;

    @Size(max = 200)
    private String component;

    @Pattern(regexp = "^[a-z]+:[a-z\\-]+$")
    private String perms;

    @Size(max = 50)
    private String icon;

    @Min(0)
    private Integer sortOrder;

    private Boolean visible;

    private Integer status;

    @Size(max = 255)
    private String remark;

}
