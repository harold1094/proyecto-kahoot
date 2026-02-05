package com.quizlive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.quizlive.model.Player;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    // Si queremos ver el ranking ordenado por puntuación
    // List<Player> findByGameRoomIdOrderByScoreDesc(Long gameRoomId);
}
