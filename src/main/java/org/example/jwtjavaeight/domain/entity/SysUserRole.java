package org.example.jwtjavaeight.domain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * 用户-角色关联实体
 */
@Getter
@Setter
@ToString
public class SysUserRole {
    /** ID */
    private Integer id;
    /** 用户ID */
    private Long userId;
    /** 角色ID */
    private Integer roleId;
    /** 创建时间 */
    private Date createTime;
}
