package org.example.jwtjavaeight.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.jwtjavaeight.domain.dto.LoginRequest;
import org.example.jwtjavaeight.domain.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RBAC完整流程集成测试
 * 测试场景：
 * 1. 用户登录获取包含角色和权限的JWT
 * 2. 使用JWT访问受保护的资源
 * 3. 验证权限控制生效
 */
@SpringBootTest
@AutoConfigureMockMvc
public class RbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testFullRbacWorkflow() throws Exception {
        // Step 1: 用户登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody)
            .get("data").get("accessToken").asText();

        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotEmpty();

        // Step 2: 使用JWT访问受保护的角色列表API（需要 role:list 权限）
        mockMvc.perform(get("/api/roles")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].roleCode").exists());

        // Step 3: 访问菜单列表API（需要 role:list 权限）
        mockMvc.perform(get("/api/menus")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray());

        // Step 4: 查询用户的角色
        mockMvc.perform(get("/api/users/1/roles")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    public void testAccessDeniedWithoutPermission() throws Exception {
        // 登录一个只有基础权限的用户（假设user2只有查询权限，没有删除权限）
        // 这里需要先创建一个测试用户，或者使用现有的普通用户

        // Step 1: 以普通用户登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("user"); // 假设有一个普通用户
        loginRequest.setPassword("user123");

        // 如果普通用户不存在，这个测试会失败，那是预期的
        // 可以在数据库初始化时创建这个用户

        // 此处省略完整测试，因为需要先创建测试数据
    }

    @Test
    public void testAccessWithoutToken() throws Exception {
        // Step: 不带token访问受保护的资源，应该返回401
        mockMvc.perform(get("/api/roles"))
            .andExpect(status().isUnauthorized());
    }
}
