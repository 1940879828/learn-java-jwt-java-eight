package org.example.jwtjavaeight.domain.entity;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityTest {

    @Test
    public void testSysRoleEntity() {
        SysRole role = new SysRole();
        role.setId(1);
        role.setRoleCode("ROLE_ADMIN");
        role.setRoleName("管理员");

        assertThat(role.getId()).isEqualTo(1);
        assertThat(role.getRoleCode()).isEqualTo("ROLE_ADMIN");
        assertThat(role.getRoleName()).isEqualTo("管理员");
        assertThat(role.toString()).contains("ROLE_ADMIN");
    }

    @Test
    public void testSysMenuEntity() {
        SysMenu menu = new SysMenu();
        menu.setId(1);
        menu.setMenuCode("system:user");
        menu.setMenuName("用户管理");
        menu.setPerms("user:list");

        assertThat(menu.getId()).isEqualTo(1);
        assertThat(menu.getMenuCode()).isEqualTo("system:user");
        assertThat(menu.getPerms()).isEqualTo("user:list");
    }

    @Test
    public void testSysUserRoleEntity() {
        SysUserRole userRole = new SysUserRole();
        userRole.setId(1);
        userRole.setUserId(1L);
        userRole.setRoleId(1);

        assertThat(userRole.getUserId()).isEqualTo(1L);
        assertThat(userRole.getRoleId()).isEqualTo(1);
    }

    @Test
    public void testSysRoleMenuEntity() {
        SysRoleMenu roleMenu = new SysRoleMenu();
        roleMenu.setId(1);
        roleMenu.setRoleId(1);
        roleMenu.setMenuId(1);

        assertThat(roleMenu.getRoleId()).isEqualTo(1);
        assertThat(roleMenu.getMenuId()).isEqualTo(1);
    }
}
