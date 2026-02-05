package com.quizlive.controller;

import com.quizlive.model.GameRoom;
import com.quizlive.model.User;
import com.quizlive.service.GameService;
import com.quizlive.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class GameController {

    @Autowired
    private com.quizlive.service.BlockService blockService;

    @Autowired
    private com.quizlive.service.GameService gameService;

    @Autowired
    private com.quizlive.service.UserService userService;

    // PASO 1: Mostrar formulario de Configuración
    @GetMapping("/game/config/{blockId}")
    public String showConfigForm(@PathVariable Long blockId, Model model) {
        com.quizlive.model.Block block = blockService.getBlockById(blockId);
        if(block == null) return "redirect:/blocks";
        
        // Objeto DTO para el formulario
        com.quizlive.dto.GameConfigForm form = new com.quizlive.dto.GameConfigForm();
        form.setBlockId(blockId);
        form.setTimeLimit(20); // Valor por defecto
        
        model.addAttribute("configForm", form);
        model.addAttribute("block", block);
        return "game/config";
    }

    // PASO 2: Recibir Configuración -> Crear Sala
    @PostMapping("/game/create")
    public String createGame(@ModelAttribute com.quizlive.dto.GameConfigForm form) {
        User host = userService.getOrCreateMockUser();
        GameRoom room = gameService.createGameWithConfig(form, host);
        return "redirect:/game/lobby/" + room.getPin();
    }
    
    // ... Resto igual ...

    // Pantalla de Lobby (Host)
    @GetMapping("/game/lobby/{pin}")
    public String showLobby(@PathVariable String pin, Model model) {
        Optional<GameRoom> roomOpt = gameService.getRoomByPin(pin);
        if(roomOpt.isPresent()) {
            model.addAttribute("room", roomOpt.get());
            model.addAttribute("joinUrl", "http://localhost:8080/play"); 
            return "host/lobby";
        }
        return "redirect:/blocks";
    }

    // Acción "Comenzar Juego"
    @PostMapping("/game/start")
    public String startGame(@RequestParam String pin) {
        gameService.startGame(pin);
        return "redirect:/game/play/host/" + pin;
    }

    // Pantalla de Juego (Host) - Muestra la pregunta actual
    @GetMapping("/game/play/host/{pin}")
    public String showGameHost(@PathVariable String pin, Model model) {
        Optional<GameRoom> roomOpt = gameService.getRoomByPin(pin);
        if(roomOpt.isPresent()) {
            GameRoom room = roomOpt.get();
            
            if("FINISHED".equals(room.getStatus())) {
                return "redirect:/game/ranking/" + pin;
            }
            if(!room.getStatus().equals("PLAYING")) {
                 return "redirect:/game/lobby/" + pin; 
            }
            
            // Obtener pregunta actual desde RoomQuestions
            com.quizlive.model.RoomQuestion currentRQ = gameService.getCurrentRoomQuestion(room);
            if(currentRQ != null) {
                 model.addAttribute("room", room);
                 model.addAttribute("currentQuestion", currentRQ.getQuestion()); // Pasamos la Question dentro del RQ
                 return "host/game";
            } else {
                return "redirect:/game/ranking/" + pin; 
            }
        }
        return "redirect:/blocks";
    }
    
    // --- FLUJO DE JUEGO (Host) ---
    
    // Avanzar a la siguiente pregunta
    @PostMapping("/game/next")
    public String nextQuestion(@RequestParam String pin) {
        boolean hasMore = gameService.nextQuestion(pin);
        if(hasMore) {
            return "redirect:/game/play/host/" + pin;
        } else {
            return "redirect:/game/ranking/" + pin;
        }
    }
    
    // Pantalla Ranking Final
    @GetMapping("/game/ranking/{pin}")
    public String showRanking(@PathVariable String pin, Model model) {
        Optional<GameRoom> roomOpt = gameService.getRoomByPin(pin);
        if(roomOpt.isPresent()) {
            model.addAttribute("room", roomOpt.get());
            model.addAttribute("players", gameService.getRanking(roomOpt.get()));
            return "host/ranking";
        }
        return "redirect:/blocks";
    }

    // --- ZONA JUGADOR ---

    // 1. Pantalla Login (Meter PIN)
    @GetMapping("/play")
    public String showPlayerLogin() {
        return "player/login";
    }

    // 2. Procesar Login
    @PostMapping("/play/join")
    public String joinGame(@RequestParam String pin, @RequestParam String nickname,  Model model) {
        com.quizlive.model.Player player = gameService.joinGame(pin, nickname);
        if (player != null) {
            // Si el login es correcto, enviamos al alumno a la sala de espera
            return "redirect:/play/wait/" + player.getId();
        }
        // Si falla (pin mal o sala cerrada), volvemos al login con error
        return "redirect:/play?error=true";
    }

    // 3. Pantalla de Espera / Juego del Alumno
    @GetMapping("/play/wait/{playerId}")
    public String showWaitingScreen(@PathVariable Long playerId, Model model) {
        com.quizlive.model.Player player = gameService.getPlayerById(playerId);
        if(player == null) return "redirect:/play";
        
        GameRoom room = player.getGameRoom();
        
        if("PLAYING".equals(room.getStatus())) {
            // Si ya están jugando, ¡a los botones!
            return "redirect:/play/game/" + playerId;
        }
        if("FINISHED".equals(room.getStatus())) {
            // Si acabó, mostrar pantalla final
             model.addAttribute("player", player);
             model.addAttribute("position", gameService.getPlayerPosition(player));
             model.addAttribute("totalPlayers", gameService.getTotalPlayersInRoom(player));
             return "player/finished";
        }

        // Si sigue en LOBBY, mostrar espera
        model.addAttribute("playerId", playerId);
        model.addAttribute("player", player);
        return "player/wait";
    }
    
    // 4. Pantalla de Juego (KAHOOT: muestra pregunta de la sala)
    @GetMapping("/play/game/{playerId}")
    public String showPlayerGame(@PathVariable Long playerId, Model model) {
        com.quizlive.model.Player player = gameService.getPlayerById(playerId);
        if(player == null) return "redirect:/play";
        
        GameRoom room = player.getGameRoom();
        if(!"PLAYING".equals(room.getStatus())) {
             return "redirect:/play/wait/" + playerId;
        }
        
        // Verificar si el juego ha terminado
        if("FINISHED".equals(room.getStatus())) {
            model.addAttribute("player", player);
            model.addAttribute("position", gameService.getPlayerPosition(player));
            model.addAttribute("totalPlayers", gameService.getTotalPlayersInRoom(player));
            return "player/finished";
        }
        
        // KAHOOT: Obtener la pregunta actual DE LA SALA (todos ven la misma)
        com.quizlive.model.RoomQuestion currentRQ = gameService.getCurrentRoomQuestion(room);
        if(currentRQ == null) {
            model.addAttribute("player", player);
            model.addAttribute("position", gameService.getPlayerPosition(player));
            model.addAttribute("totalPlayers", gameService.getTotalPlayersInRoom(player));
            return "player/finished";
        }
        
        // KAHOOT: Si el jugador ya respondió esta pregunta, mostrar pantalla de espera
        if(gameService.hasPlayerAnsweredCurrentQuestion(player)) {
            model.addAttribute("playerId", playerId);
            model.addAttribute("isCorrect", gameService.wasPlayerAnswerCorrect(player));
            return "player/answered";
        }
        
        int totalQuestions = gameService.getTotalQuestionsForRoom(room);
        
        model.addAttribute("player", player);
        model.addAttribute("currentQuestion", currentRQ.getQuestion());
        model.addAttribute("questionNumber", room.getCurrentQuestionIndex() + 1);
        model.addAttribute("totalQuestions", totalQuestions);
        model.addAttribute("timeLimit", room.getTimeLimit());
        return "player/game"; 
    }

    // 5. Recibir respuesta (KAHOOT: redirige a pantalla de espera)
    @PostMapping("/play/answer")
    public String submitAnswer(@RequestParam Long playerId, @RequestParam int optionIndex, Model model) {
        boolean isCorrect = gameService.submitAnswer(playerId, optionIndex);
        // Pasar el playerId y resultado para la pantalla de espera
        model.addAttribute("playerId", playerId);
        model.addAttribute("isCorrect", isCorrect);
        // KAHOOT: Redirigir a pantalla de "respuesta enviada, esperando al profe"
        return "player/answered";
    }
}
