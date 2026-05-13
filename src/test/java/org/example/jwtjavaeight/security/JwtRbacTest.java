package org.example.jwtjavaeight.security;

import io.jsonwebtoken.Claims;
import org.example.jwtjavaeight.mapper.RoleMapper;
import org.example.jwtjavaeight.mapper.UserMapper;
import org.example.jwtjavaeight.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class JwtRbacTest {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    public void testUserDetailsContainsRoles() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities()).isNotEmpty();

        boolean hasRoleAdmin = userDetails.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        assertThat(hasRoleAdmin).isTrue();
    }

    @Test
    public void testUserDetailsContainsPermissions() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        boolean hasUserListPerm = userDetails.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("user:list"));
        assertThat(hasUserListPerm).isTrue();
    }

    @Test
    public void testJwtTokenContainsRolesAndPermissions() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");
        String token = jwtUtil.generateAccessToken(userDetails);

        assertThat(token).isNotNull();

        Claims claims = jwtUtil.parseToken(token);
        @SuppressWarnings("unchecked")
        List<String> authorities = (List<String>) claims.get("authorities");

        assertThat(authorities).isNotNull();
        assertThat(authorities).contains("ROLE_ADMIN");
        assertThat(authorities).contains("user:list");
    }
}
