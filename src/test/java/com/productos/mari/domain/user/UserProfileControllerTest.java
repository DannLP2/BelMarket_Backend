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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock private UserService userService;

    @InjectMocks
    private UserProfileController userProfileController;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userProfileController).build();
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@test.com");
    }

    @Test
    void getProfile_Success() throws Exception {
        UserProfileDto dto = UserProfileDto.builder()
                .user(mockUser)
                .build();
        when(userService.getCurrentUserProfile()).thenReturn(dto);

        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("test@test.com"));
    }

    @Test
    void updateDefaultDashboard_Success() throws Exception {
        when(userService.updateDefaultDashboard(DashboardType.ADMIN)).thenReturn(mockUser);

        mockMvc.perform(patch("/api/users/profile/default-dashboard")
                .param("dashboardType", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void updateProfile_Success() throws Exception {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setName("New Name");
        
        when(userService.updateProfile(any())).thenReturn(mockUser);

        mockMvc.perform(put("/api/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void updateProfilePicture_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "profile.jpg", "image/jpeg", "content".getBytes());
        when(userService.updateProfilePicture(any())).thenReturn(mockUser);

        mockMvc.perform(multipart("/api/users/profile/picture")
                .file(file))
                .andExpect(status().isOk());
    }
}
