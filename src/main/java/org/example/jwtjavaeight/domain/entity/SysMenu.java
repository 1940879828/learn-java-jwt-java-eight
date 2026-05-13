package org.example.jwtjavaeight.domain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * 系统菜单/权限实体
 */
@Getter
@Setter
@ToString
public class SysMenu {
    /** 菜单ID */
    private Integer id;
    /** 父菜单ID */
    private Integer parentId;
    /** 菜单名称 */
    private String menuName;
    /** 菜单编码/权限标识 */
    private String menuCode;
    /** 菜单类型：1-菜单，2-按钮，3-接口 */
    private Integer menuType;
    /** 路由路径 */
    private String path;
    /** 组件路径 */
    private String component;
    /** 权限标识 */
    private String perms;
    /** 菜单图标 */
    private String icon;
    /** 排序 */
    private Integer sortOrder;
    /** 是否可见：0-隐藏，1-显示 */
    private Integer visible;
    /** 状态：0-禁用，1-启用 */
    private Integer status;
    /** 创建者 */
    private String createBy;
    /** 创建时间 */
    private Date createTime;
    /** 备注 */
    private String remark;
}
