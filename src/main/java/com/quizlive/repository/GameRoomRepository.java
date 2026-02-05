package com.quizlive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.quizlive.model.GameRoom;
import java.util.Optional;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
    // Buscar sala por PIN (para cuando el alumno mete el código)
    Optional<GameRoom> findByPin(String pin);
}
