package com.quizlive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.quizlive.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    boolean existsByUsername(String username);
}
