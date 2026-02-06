package com.quizlive.controller;

import com.quizlive.model.User;
import com.quizlive.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String showLogin(@RequestParam(required = false) String error,
                           @RequestParam(required = false) String logout,
                           @RequestParam(required = false) String registered,
                           Model model) {
        if (error != null) {
            model.addAttribute("error", "Usuario o contraseña incorrectos");
        }
        if (logout != null) {
            model.addAttribute("message", "Has cerrado sesión correctamente");
        }
        if (registered != null) {
            model.addAttribute("message", "¡Registro exitoso! Ya puedes iniciar sesión");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegister() {
        return "auth/register";
    }

    @PostMapping("/auth/register")
    public String processRegister(@RequestParam String username,
                                  @RequestParam String password,
                                  @RequestParam String confirmPassword,
                                  Model model) {
        // Validaciones
        if (username == null || username.trim().isEmpty()) {
            model.addAttribute("error", "El nombre de usuario es obligatorio");
            return "auth/register";
        }
        
        if (password == null || password.length() < 4) {
            model.addAttribute("error", "La contraseña debe tener al menos 4 caracteres");
            return "auth/register";
        }
        
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden");
            return "auth/register";
        }
        
        if (userService.existsByUsername(username)) {
            model.addAttribute("error", "Ese nombre de usuario ya existe");
            return "auth/register";
        }
        
        // Registrar usuario
        User user = userService.registerUser(username, password);
        if (user != null) {
            return "redirect:/login?registered=true";
        } else {
            model.addAttribute("error", "Error al registrar usuario");
            return "auth/register";
        }
    }
}
