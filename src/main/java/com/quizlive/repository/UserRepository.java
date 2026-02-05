package com.quizlive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.quizlive.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    // Aquí podemos añadir métodos de búsqueda personalizados si hiciera falta
    // Ejemplo: User findByUsername(String username);
}
