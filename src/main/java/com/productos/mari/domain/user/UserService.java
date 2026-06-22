package com.productos.mari.domain.user;

import com.productos.mari.domain.user.UserUpdateRequest;
import com.productos.mari.domain.user.User;
import java.util.List;

public interface UserService {
    List<User> getAllUsers();
    User updateRoles(Long userId, java.util.Set<com.productos.mari.domain.user.Role> roles);
    User updateUser(Long userId, UserUpdateRequest request);
    User updateProfile(UserUpdateRequest request);
    User updateProfilePicture(org.springframework.web.multipart.MultipartFile file);
    User updateUserProfilePicture(Long userId, org.springframework.web.multipart.MultipartFile file);
    User toggleUserStatus(Long userId);
    User updateDefaultDashboard(DashboardType dashboardType);
    com.productos.mari.domain.user.UserProfileDto getUserProfile(Long userId);
    com.productos.mari.domain.user.UserProfileDto getCurrentUserProfile();
    void deleteUser(Long userId);
}
