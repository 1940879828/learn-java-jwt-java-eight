package org.example.jwtjavaeight.mapper;

import org.example.jwtjavaeight.domain.entity.SysMenu;
import org.example.jwtjavaeight.domain.entity.SysRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class RbacMapperTest {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private MenuMapper menuMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    public void testFindRoleById() {
        SysRole role = roleMapper.findById(1);
        assertThat(role).isNotNull();
        assertThat(role.getRoleCode()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    public void testFindRolesByUserId() {
        List<SysRole> roles = roleMapper.findRolesByUserId(1L);
        assertThat(roles).isNotNull();
    }

    @Test
    public void testFindMenusByRoleId() {
        List<SysMenu> menus = menuMapper.findMenusByRoleId(1);
        assertThat(menus).isNotNull();
        assertThat(menus).isNotEmpty();
    }

    @Test
    public void testFindPermissionsByUserId() {
        List<String> permissions = userMapper.findPermissionsByUserId(1L);
        assertThat(permissions).isNotNull();
    }
}
