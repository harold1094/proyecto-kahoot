package com.quizlive.service;

import com.quizlive.model.User;
import com.quizlive.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Método temporal para obtener siempre un usuario "Login Mock"
    // Si no existe, lo crea.
    public User getOrCreateMockUser() {
        return userRepository.findById(1L).orElseGet(() -> {
            User user = new User();
            user.setUsername("host1");
            user.setPassword("1234");
            return userRepository.save(user);
        });
    }
}
