package com.quizlive.service;

import com.quizlive.model.Block;
import com.quizlive.model.GameRoom;
import com.quizlive.model.Player;
import com.quizlive.model.Question;
import com.quizlive.model.User;
import com.quizlive.repository.GameRoomRepository;
import com.quizlive.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Random;
import java.util.Optional;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class GameService {

    @Autowired
    private com.quizlive.repository.RoomQuestionRepository roomQuestionRepository;

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private com.quizlive.engine.QuizEngine quizEngine;

    @Autowired
    private BlockService blockService;

    // Crear Sala con Configuración (Parte C)
    public GameRoom createGameWithConfig(com.quizlive.dto.GameConfigForm config, User host) {
        Block block = blockService.getBlockById(config.getBlockId());
        if(block == null) return null;

        GameRoom room = new GameRoom();
        room.setBlock(block);
        room.setHost(host);
        room.setStatus("LOBBY");
        room.setCurrentQuestionIndex(0);
        room.setTimeLimit(config.getTimeLimit());
        
        // Generar PIN
        String pin;
        do {
            pin = String.format("%05d", new Random().nextInt(100000));
        } while (gameRoomRepository.findByPin(pin).isPresent());
        room.setPin(pin);
        
        room = gameRoomRepository.save(room);
        
        // --- INTEGRACIÓN ENGINE: Inicializar sala en memoria ---
        quizEngine.initRoom(pin, room.getId());

        // Selección de Preguntas (Parte C / E)
        List<Question> selectedQuestions = new java.util.ArrayList<>();
        List<Question> allQuestions = block.getQuestions();

        if(config.isRandomMode()) {
            // Modo Aleatorio: Barajar y coger N
            java.util.Collections.shuffle(allQuestions);
            int n = Math.min(config.getNumQuestionsRandom(), allQuestions.size());
            selectedQuestions = allQuestions.subList(0, n);
        } else {
            // Modo Manual: Filtrar por IDs seleccionados
            if(config.getSelectedQuestionIds() != null) {
                for(Question q : allQuestions) {
                    if(config.getSelectedQuestionIds().contains(q.getId())) {
                        selectedQuestions.add(q);
                    }
                }
            }
        }
        
        // Guardar RoomQuestion (Orden fijo para esta partida)
        int order = 0;
        for(Question q : selectedQuestions) {
            com.quizlive.model.RoomQuestion rq = new com.quizlive.model.RoomQuestion(room, q, order++);
            roomQuestionRepository.save(rq);
        }
        
        return room;
    }
    
    public Optional<GameRoom> getRoomByPin(String pin) {
        return gameRoomRepository.findByPin(pin);
    }

    public Player joinGame(String pin, String nickname) {
        // Parte D: Evitar nombres duplicados
        Optional<GameRoom> roomOpt = gameRoomRepository.findByPin(pin);
        if(roomOpt.isPresent()) {
            GameRoom room = roomOpt.get();
            if("LOBBY".equals(room.getStatus())) {
                // Verificar duplicado
                boolean exists = room.getPlayers().stream()
                        .anyMatch(p -> p.getNickname().equalsIgnoreCase(nickname));
                if(exists) return null; // O lanzar excepción personalizada

                Player player = new Player();
                player.setNickname(nickname);
                player.setScore(0);
                player.setGameRoom(room);
                return playerRepository.save(player);
            }
        }
        return null; 
    }

    public void startGame(String pin) {
        Optional<GameRoom> roomOpt = gameRoomRepository.findByPin(pin);
        if(roomOpt.isPresent()) {
            GameRoom room = roomOpt.get();
            room.setStatus("PLAYING");
            room.setCurrentQuestionIndex(0);
            room.setCurrentQuestionStartTime(java.time.LocalDateTime.now()); // Marca de tiempo inicio
            gameRoomRepository.save(room);
            
            // --- INTEGRACIÓN ENGINE: Iniciar Timer pregunta 1 ---
            quizEngine.startQuestionTimer(pin, room.getTimeLimit());
        }
    }
    
    public Player getPlayerById(Long id) {
        return playerRepository.findById(id).orElse(null);
    }
    
    // Calcular la posición del jugador en el ranking
    public int getPlayerPosition(Player player) {
        GameRoom room = player.getGameRoom();
        java.util.List<Player> players = new java.util.ArrayList<>(room.getPlayers());
        // Ordenar por puntuación descendente
        players.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        
        for(int i = 0; i < players.size(); i++) {
            if(players.get(i).getId().equals(player.getId())) {
                return i + 1; // Posición 1-indexed
            }
        }
        return players.size();
    }
    
    // Obtener total de jugadores en la sala
    public int getTotalPlayersInRoom(Player player) {
        return player.getGameRoom().getPlayers().size();
    }

    // --- LÓGICA DE JUEGO ESTILO QUIZIZZ ---

    // Helper para obtener la pregunta actual DE LA SALA (para el host)
    public com.quizlive.model.RoomQuestion getCurrentRoomQuestion(GameRoom room) {
        List<com.quizlive.model.RoomQuestion> questions = roomQuestionRepository.findByGameRoomOrderByOrderIndexAsc(room);
        if(room.getCurrentQuestionIndex() < questions.size()) {
            return questions.get(room.getCurrentQuestionIndex());
        }
        return null;
    }
    
    // Helper para obtener la pregunta actual DEL JUGADOR (Quizizz: cada uno a su ritmo)
    public com.quizlive.model.RoomQuestion getCurrentRoomQuestionForPlayer(Player player) {
        GameRoom room = player.getGameRoom();
        List<com.quizlive.model.RoomQuestion> questions = roomQuestionRepository.findByGameRoomOrderByOrderIndexAsc(room);
        if(player.getCurrentQuestionIndex() < questions.size()) {
            return questions.get(player.getCurrentQuestionIndex());
        }
        return null; // El jugador ha terminado
    }
    
    // Obtener total de preguntas de la sala
    public int getTotalQuestionsForRoom(GameRoom room) {
        return roomQuestionRepository.findByGameRoomOrderByOrderIndexAsc(room).size();
    }
    
    // Verificar si el jugador ha terminado todas las preguntas
    public boolean hasPlayerFinished(Player player) {
        int total = getTotalQuestionsForRoom(player.getGameRoom());
        return player.getCurrentQuestionIndex() >= total;
    }
    
    @Autowired
    private com.quizlive.repository.AnswerRepository answerRepository;
    
    // Verificar si el jugador ya respondió la pregunta actual de la sala
    public boolean hasPlayerAnsweredCurrentQuestion(Player player) {
        GameRoom room = player.getGameRoom();
        com.quizlive.model.RoomQuestion currentRQ = getCurrentRoomQuestion(room);
        if(currentRQ == null) return false;
        
        // Buscar si existe una respuesta de este jugador para esta RoomQuestion
        return answerRepository.existsByPlayerAndRoomQuestion(player, currentRQ);
    }
    
    // Obtener si la respuesta del jugador a la pregunta actual fue correcta
    public boolean wasPlayerAnswerCorrect(Player player) {
        GameRoom room = player.getGameRoom();
        com.quizlive.model.RoomQuestion currentRQ = getCurrentRoomQuestion(room);
        if(currentRQ == null) return false;
        
        java.util.Optional<com.quizlive.model.Answer> answer = answerRepository.findByPlayerAndRoomQuestion(player, currentRQ);
        return answer.map(com.quizlive.model.Answer::isCorrect).orElse(false);
    }

    public boolean submitAnswer(Long playerId, int optionIndex) {
        Player player = playerRepository.findById(playerId).orElse(null);
        if(player != null) {
            GameRoom room = player.getGameRoom();
            if("PLAYING".equals(room.getStatus())) {
                
                // KAHOOT: Obtenemos la pregunta de la SALA (todos ven la misma)
                com.quizlive.model.RoomQuestion currentRQ = getCurrentRoomQuestion(room);
                if(currentRQ != null) {
                   Question q = currentRQ.getQuestion();
                   boolean isCorrect = (q.getCorrectOptionIndex() == optionIndex);
                   
                   // --- INTEGRACIÓN ENGINE: Procesar respuesta ASYNC ---
                   quizEngine.processAnswerAsync(room.getPin(), playerId, isCorrect, () -> {
                       // Callback de Persistencia
                       saveAnswerToDb(player, currentRQ, optionIndex, isCorrect);
                   });
                   
                   // KAHOOT: NO avanzamos al jugador. Espera a que el profe pase.
                   return isCorrect;
                }
            }
        }
        return false;
    }
    
    // QUIZIZZ: Avanzar jugador a la siguiente pregunta
    public void advancePlayer(Player player) {
        int nextIndex = player.getCurrentQuestionIndex() + 1;
        player.setCurrentQuestionIndex(nextIndex);
        playerRepository.save(player);
        
        // Verificar si el jugador terminó todas las preguntas
        if(hasPlayerFinished(player)) {
            System.out.println("[QUIZIZZ] Jugador " + player.getNickname() + " ha TERMINADO el quiz!");
        }
    }
    
    // Método auxiliar para persistir (invocado desde el hilo async)
    private void saveAnswerToDb(Player player, com.quizlive.model.RoomQuestion rq, int optionIndex, boolean isCorrect) {
       // Guardar Respuesta
       com.quizlive.model.Answer answer = new com.quizlive.model.Answer();
       answer.setPlayer(player);
       answer.setRoomQuestion(rq);
       answer.setSelectedOption(optionIndex);
       answer.setCorrect(isCorrect);
       answerRepository.save(answer);

       // Actualizar Puntuación en DB
       if(isCorrect) {
           player.setScore(player.getScore() + 1); 
           playerRepository.save(player);
       }
    }

    public boolean nextQuestion(String pin) {
        Optional<GameRoom> roomOpt = gameRoomRepository.findByPin(pin);
        if(roomOpt.isPresent()) {
            GameRoom room = roomOpt.get();
            int nextIndex = room.getCurrentQuestionIndex() + 1;
            
            // Verificar contra el número de RoomQuestions
            int totalQuestions = roomQuestionRepository.findByGameRoomOrderByOrderIndexAsc(room).size();
            
            if (nextIndex < totalQuestions) {
                room.setCurrentQuestionIndex(nextIndex);
                room.setCurrentQuestionStartTime(java.time.LocalDateTime.now()); 
                gameRoomRepository.save(room);
                
                // --- INTEGRACIÓN ENGINE: Timer siguiente pregunta ---
                quizEngine.startQuestionTimer(pin, room.getTimeLimit());
                
                return true; 
            } else {
                room.setStatus("FINISHED");
                gameRoomRepository.save(room);
                return false; 
            }
        }
        return false;
    }
    
    public List<Player> getRanking(GameRoom room) {
        // Podríamos sacar el ranking de memoria del Engine si quisiéramos tiempo real absoluto,
        // pero para persistencia y simplicidad final leemos de BD.
        return room.getPlayers().stream()
                .sorted(Comparator.comparingInt(Player::getScore).reversed())
                .collect(Collectors.toList());
    }
}
