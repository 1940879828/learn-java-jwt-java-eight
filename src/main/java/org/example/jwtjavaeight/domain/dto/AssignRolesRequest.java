package org.example.jwtjavaeight.domain.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Getter
@Setter
public class AssignRolesRequest {
    @NotEmpty(message = "角色ID列表不能为空")
    private List<Long> roleIds;
}
