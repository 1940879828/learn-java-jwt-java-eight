package org.example.jwtjavaeight.service;

import org.example.jwtjavaeight.domain.dto.MenuResponse;
import org.example.jwtjavaeight.domain.dto.RoleResponse;
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

    @Test
    public void testFindAllRoles() {
        List<SysRole> roles = roleService.findAll();
        assertThat(roles).isNotNull();
        assertThat(roles).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    public void testFindRoleById() {
        RoleResponse role = roleService.findById(1);
        assertThat(role).isNotNull();
        assertThat(role.getRoleCode()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    public void testFindRolesByUserId() {
        List<SysRole> roles = roleService.findRolesByUserId(1L);
        assertThat(roles).isNotNull();
    }

    @Test
    public void testReplaceRoleMenus() {
        List<Integer> menuIds = Arrays.asList(1, 2, 3);
        roleService.replaceRoleMenus(1, menuIds);

        List<MenuResponse> menus = roleService.findMenusByRoleId(1);
        assertThat(menus).hasSizeGreaterThanOrEqualTo(3);
    }
}
