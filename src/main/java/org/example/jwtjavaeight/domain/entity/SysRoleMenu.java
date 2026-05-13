package org.example.jwtjavaeight.domain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * 角色-菜单关联实体
 */
@Getter
@Setter
@ToString
public class SysRoleMenu {
    /** ID */
    private Integer id;
    /** 角色ID */
    private Integer roleId;
    /** 菜单ID */
    private Integer menuId;
    /** 创建时间 */
    private Date createTime;
}
