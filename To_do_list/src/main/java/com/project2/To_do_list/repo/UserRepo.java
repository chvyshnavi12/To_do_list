package com.project2.To_do_list.repo;

import com.project2.To_do_list.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long> {

    // For login (if you’re doing manual authentication)
    User findByEmailAndPassword(String email, String password);

    // For fetching user after login
    Optional<User> findByEmail(String email);
}
