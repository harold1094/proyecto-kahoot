package com.quizlive.controller;

import com.quizlive.model.Block;
import com.quizlive.model.User;
import com.quizlive.service.BlockService;
import com.quizlive.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/blocks")
public class BlockController {

    @Autowired
    private BlockService blockService;

    @Autowired
    private UserService userService;

    // Listar Mis Bloques
    @GetMapping
    public String listBlocks(Model model) {
        User currentUser = userService.getCurrentUser();
        model.addAttribute("blocks", blockService.getBlocksByUser(currentUser));
        model.addAttribute("currentUser", currentUser);
        return "blocks/list";
    }

    // Formulario Crear Bloque
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("block", new Block());
        return "blocks/form";
    }

    // Formulario Editar Bloque
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        User currentUser = userService.getCurrentUser();
        Block block = blockService.getBlockById(id);
        
        // Validar que el bloque pertenezca al usuario actual
        if (block == null || block.getOwner() == null || !block.getOwner().getId().equals(currentUser.getId())) {
            return "redirect:/blocks";
        }
        
        model.addAttribute("block", block);
        return "blocks/form";
    }

    // Guardar Bloque
    @PostMapping("/save")
    public String saveBlock(@ModelAttribute Block block) {
        User currentUser = userService.getCurrentUser();
        blockService.saveBlock(block, currentUser);
        return "redirect:/blocks";
    }

    // Borrar Bloque
    @GetMapping("/delete/{id}")
    public String deleteBlock(@PathVariable Long id) {
        User currentUser = userService.getCurrentUser();
        Block block = blockService.getBlockById(id);
        
        // Validar que el bloque pertenezca al usuario actual
        if (block != null && block.getOwner() != null && block.getOwner().getId().equals(currentUser.getId())) {
            blockService.deleteBlock(id);
        }
        
        return "redirect:/blocks";
    }
}
