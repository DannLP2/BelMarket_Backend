package com.productos.mari.domain.user;

import com.productos.mari.domain.user.UserUpdateRequest;
import com.productos.mari.domain.user.User;
import com.productos.mari.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;
    
    @org.springframework.web.bind.annotation.GetMapping
    public ResponseEntity<com.productos.mari.domain.user.UserProfileDto> getProfile() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }

    @org.springframework.web.bind.annotation.PatchMapping("/default-dashboard")
    public ResponseEntity<User> updateDefaultDashboard(@RequestParam com.productos.mari.domain.user.DashboardType dashboardType) {
        return ResponseEntity.ok(userService.updateDefaultDashboard(dashboardType));
    }

    @PutMapping
    public ResponseEntity<User> updateProfile(@Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @PostMapping("/picture")
    public ResponseEntity<User> updateProfilePicture(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.updateProfilePicture(file));
    }
}
