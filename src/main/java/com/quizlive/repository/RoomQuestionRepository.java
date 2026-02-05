package com.quizlive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.quizlive.model.RoomQuestion;
import com.quizlive.model.GameRoom;
import java.util.List;

public interface RoomQuestionRepository extends JpaRepository<RoomQuestion, Long> {
    // Buscar preguntas de una sala ordenadas
    List<RoomQuestion> findByGameRoomOrderByOrderIndexAsc(GameRoom gameRoom);
}
