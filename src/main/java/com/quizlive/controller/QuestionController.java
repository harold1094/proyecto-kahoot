package com.quizlive.controller;

import com.quizlive.model.Question;
import com.quizlive.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@Controller
@RequestMapping("/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    // Formulario Crear Pregunta para un Bloque específico
    @GetMapping("/new/{blockId}")
    public String showCreateForm(@PathVariable Long blockId, Model model) {
        Question question = new Question();
        // Inicializamos la lista de opciones vacía para evitar nulos en thymeleaf
        question.setOptions(new ArrayList<>()); 
        for(int i=0; i<4; i++) question.getOptions().add(""); // Añadimos 4 espacios vacíos

        model.addAttribute("question", question);
        model.addAttribute("blockId", blockId);
        return "questions/form";
    }

    // Guardar Pregunta
    @PostMapping("/save")
    public String saveQuestion(@ModelAttribute Question question, @RequestParam Long blockId) {
        System.out.println("Guardando Pregunta: " + question.getStatement());
        System.out.println("Opciones recibidas: " + (question.getOptions() != null ? question.getOptions().size() : "null"));
        
        questionService.saveQuestion(question, blockId);
        // Redirigimos a la edición del bloque para ver las preguntas
        return "redirect:/blocks/edit/" + blockId; 
    }

    // Formulario Editar Pregunta
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Question question = questionService.getQuestionById(id);
        if (question != null) {
            model.addAttribute("question", question);
            model.addAttribute("blockId", question.getBlock().getId());
            return "questions/form";
        }
        return "redirect:/blocks";
    }

    // Borrar Pregunta
    @GetMapping("/delete/{id}")
    public String deleteQuestion(@PathVariable Long id, @RequestParam Long blockId) {
        questionService.deleteQuestion(id);
        return "redirect:/blocks/edit/" + blockId;
    }
}
