package org.example.jwtjavaeight.service;

import org.example.jwtjavaeight.domain.entity.SysMenu;
import org.example.jwtjavaeight.domain.entity.SysRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class RoleServiceTest {

    @Autowired
    private RoleService roleService;

    @Autowired
    private MenuService menuService;

    @Test
    public void testFindAllRoles() {
        List<SysRole> roles = roleService.findAll();
        assertThat(roles).isNotNull();
        assertThat(roles).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    public void testFindRoleById() {
        SysRole role = roleService.findById(1);
        assertThat(role).isNotNull();
        assertThat(role.getRoleCode()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    public void testFindRolesByUserId() {
        List<SysRole> roles = roleService.findRolesByUserId(1L);
        assertThat(roles).isNotNull();
    }

    @Test
    public void testAssignMenusToRole() {
        List<Integer> menuIds = Arrays.asList(1, 2, 3);
        roleService.assignMenusToRole(1, menuIds);

        List<SysMenu> menus = menuService.findMenusByRoleId(1);
        assertThat(menus).hasSizeGreaterThanOrEqualTo(3);
    }
}
