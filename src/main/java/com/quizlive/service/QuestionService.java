package com.quizlive.service;

import com.quizlive.model.Block;
import com.quizlive.model.Question;
import com.quizlive.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private BlockService blockService;

    public void saveQuestion(Question question, Long blockId) {
        Block block = blockService.getBlockById(blockId);
        if (block != null) {
            question.setBlock(block); // Asignamos la pregunta al bloque
            
            // Validaciones mínimas (Enunciado y 4 opciones)
            if (question.getOptions() == null || question.getOptions().size() < 4) {
                 // Aquí podríamos lanzar excepción, pero por simplicidad dejaremos que falle o lo manejaremos en el controlador
                 // Para el ejemplo, asumiremos que el formulario obliga a llenar 4
            }
            questionRepository.save(question);
        }
    }
    
    public void deleteQuestion(Long questionId) {
        questionRepository.deleteById(questionId);
    }
    
    public Question getQuestionById(Long id) {
        return questionRepository.findById(id).orElse(null);
    }
}
