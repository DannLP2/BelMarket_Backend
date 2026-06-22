package com.productos.mari.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock private UserService userService;

    @InjectMocks
    private UserController userController;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@test.com");
    }

    @Test
    void getAllUsers_Success() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(mockUser));

        mockMvc.perform(get("/api/users/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("test@test.com"));
    }

    @Test
    void updateRoles_Success() throws Exception {
        Set<Role> roles = Set.of(Role.ADMIN);
        when(userService.updateRoles(eq(1L), any())).thenReturn(mockUser);

        mockMvc.perform(patch("/api/users/admin/1/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roles)))
                .andExpect(status().isOk());
    }

    @Test
    void toggleStatus_Success() throws Exception {
        when(userService.toggleUserStatus(1L)).thenReturn(mockUser);

        mockMvc.perform(patch("/api/users/admin/1/status"))
                .andExpect(status().isOk());
    }

    @Test
    void updateProfilePicture_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "bytes".getBytes());
        when(userService.updateUserProfilePicture(eq(1L), any())).thenReturn(mockUser);

        mockMvc.perform(multipart("/api/users/admin/1/profile-picture")
                .file(file))
                .andExpect(status().isOk());
    }

    @Test
    void deleteUser_Success() throws Exception {
        mockMvc.perform(delete("/api/users/admin/1"))
                .andExpect(status().isNoContent());
        
        verify(userService).deleteUser(1L);
    }
}
