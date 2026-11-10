package com.edu.registration.repository;

import com.edu.registration.entity.Role;
import com.edu.registration.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    List<User> findByRoleInAndTrangThaiTrueOrderByUsernameAsc(List<Role> roles);

    List<User> findByRoleAndTrangThaiTrueOrderByUsernameAsc(Role role);
}
