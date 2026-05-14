package org.example.jwtjavaeight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.jwtjavaeight.domain.dto.AssignRolesRequest;
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
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "user:edit"})
    public void testAssignRolesToUser() throws Exception {
        AssignRolesRequest request = new AssignRolesRequest();
        request.setRoleIds(Arrays.asList(1L, 2L));

        mockMvc.perform(post("/api/users/1/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "user:list"})
    public void testGetUserRoles() throws Exception {
        mockMvc.perform(get("/api/users/1/roles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray());
    }
}
