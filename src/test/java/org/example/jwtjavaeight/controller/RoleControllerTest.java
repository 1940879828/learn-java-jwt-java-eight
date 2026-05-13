package org.example.jwtjavaeight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.jwtjavaeight.domain.dto.AssignMenusRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "role:list"})
    public void testGetAllRoles() throws Exception {
        mockMvc.perform(get("/api/roles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "role:list"})
    public void testGetRoleById() throws Exception {
        mockMvc.perform(get("/api/roles/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.roleCode").value("ROLE_ADMIN"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "role:edit"})
    public void testAssignMenusToRole() throws Exception {
        AssignMenusRequest request = new AssignMenusRequest();
        request.setMenuIds(Arrays.asList(1, 2, 3));

        mockMvc.perform(post("/api/roles/1/menus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testGetAllRolesWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/roles"))
            .andExpect(status().isUnauthorized());
    }
}
