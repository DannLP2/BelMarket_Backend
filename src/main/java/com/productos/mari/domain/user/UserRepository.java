package com.productos.mari.domain.user;

import com.productos.mari.domain.user.Role;
import com.productos.mari.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Query("SELECT u.profilePictureUrl FROM User u WHERE u.profilePictureUrl IS NOT NULL")
    List<String> findAllProfilePictureUrls();

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r IN :roles AND u.enabled = true")
    List<User> findByRolesIn(Collection<Role> roles);
}
