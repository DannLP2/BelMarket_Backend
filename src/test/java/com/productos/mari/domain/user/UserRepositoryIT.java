package com.productos.mari.domain.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class UserRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    private User activeAdmin;
    private User activeClient;
    private User disabledUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        activeAdmin = userRepository.save(User.builder()
                .name("Admin User")
                .email("admin@test.com")
                .password("password")
                .roles(Set.of(Role.ADMIN))
                .status(UserStatus.ACTIVE)
                .enabled(true)
                .build());

        activeClient = userRepository.save(User.builder()
                .name("Client User")
                .email("client@test.com")
                .password("password")
                .roles(Set.of(Role.CLIENT))
                .status(UserStatus.ACTIVE)
                .enabled(true)
                .build());

        disabledUser = userRepository.save(User.builder()
                .name("Disabled User")
                .email("disabled@test.com")
                .password("password")
                .roles(Set.of(Role.CLIENT))
                .status(UserStatus.SUSPENDED)
                .enabled(false)
                .build());
        
        em.flush();
        em.clear();
    }

    @Test
    void findByEmail_shouldReturnUser() {
        Optional<User> found = userRepository.findByEmail("admin@test.com");
        assertTrue(found.isPresent());
        assertEquals("Admin User", found.get().getName());
    }

    @Test
    void findByEmail_withNonExistingEmail_shouldReturnEmpty() {
        Optional<User> found = userRepository.findByEmail("nonexisting@test.com");
        assertFalse(found.isPresent());
    }

    @Test
    void findByRolesIn_shouldReturnOnlyEnabledUsersWithSpecifiedRoles() {
        List<User> result = userRepository.findByRolesIn(Set.of(Role.CLIENT));

        // Should return activeClient but not disabledUser (since it's not enabled in the query logic or status is ACTIVE)
        // Wait, the query in UserRepository is:
        // @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r IN :roles AND u.enabled = true")
        
        assertEquals(1, result.size());
        assertEquals("Client User", result.get(0).getName());
    }

    @Test
    void findAllProfilePictureUrls_shouldReturnNonNullUrls() {
        User userWithPic = User.builder()
                .name("Pic User")
                .email("pic@test.com")
                .password("password")
                .profilePictureUrl("http://img.com/pic.png")
                .build();
        userRepository.save(userWithPic);
        em.flush();

        List<String> urls = userRepository.findAllProfilePictureUrls();
        
        assertEquals(1, urls.size());
        assertEquals("http://img.com/pic.png", urls.get(0));
    }
}
