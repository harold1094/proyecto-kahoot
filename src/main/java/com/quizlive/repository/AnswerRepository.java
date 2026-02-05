package com.quizlive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.quizlive.model.Answer;
import com.quizlive.model.Player;
import com.quizlive.model.RoomQuestion;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    boolean existsByPlayerAndRoomQuestion(Player player, RoomQuestion roomQuestion);
    Optional<Answer> findByPlayerAndRoomQuestion(Player player, RoomQuestion roomQuestion);
}
