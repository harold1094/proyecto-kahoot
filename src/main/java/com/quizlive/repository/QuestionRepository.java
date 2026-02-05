package com.quizlive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.quizlive.model.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    // Si queremos buscar todas las preguntas de un bloque
    // Aunque normalmente accederemos a ellas a través de Block.getQuestions()
}
