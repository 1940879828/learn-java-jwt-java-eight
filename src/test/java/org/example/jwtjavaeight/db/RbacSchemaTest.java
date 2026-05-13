package org.example.jwtjavaeight.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class RbacSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testSysRoleTableExists() {
        String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                     "WHERE table_schema = 'jwt_java_eight' AND table_name = 'sys_role'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testSysMenuTableExists() {
        String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                     "WHERE table_schema = 'jwt_java_eight' AND table_name = 'sys_menu'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testSysUserRoleTableExists() {
        String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                     "WHERE table_schema = 'jwt_java_eight' AND table_name = 'sys_user_role'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testSysRoleMenuTableExists() {
        String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                     "WHERE table_schema = 'jwt_java_eight' AND table_name = 'sys_role_menu'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
