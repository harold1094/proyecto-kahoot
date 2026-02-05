package com.quizlive.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.ArrayList;

@Entity
public class GameRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pin; // Código de acceso (ej: "48291")
    
    // Estados: "LOBBY" (Esperando), "PLAYING" (Jugando), "FINISHED" (Acabado)
    private String status; 
    
    private int currentQuestionIndex; // 0, 1, 2... para saber en qué pregunta vamos

    // ¿Qué bloque estamos jugando?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private User host;

    // Configuración de la sala
    private int timeLimit; // Segundos por pregunta
    
    private java.time.LocalDateTime currentQuestionStartTime; // Cuando empezó la pregunta actual

    // Relación OneToMany con RoomQuestion (las preguntas elegidas)

    // Relación OneToMany con RoomQuestion (las preguntas elegidas)
    @OneToMany(mappedBy = "gameRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomQuestion> roomQuestions = new ArrayList<>();

    // Lista de jugadores conectados
    @OneToMany(mappedBy = "gameRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Player> players = new ArrayList<>();

    public GameRoom() {
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getCurrentQuestionIndex() { return currentQuestionIndex; }
    public void setCurrentQuestionIndex(int currentQuestionIndex) { this.currentQuestionIndex = currentQuestionIndex; }

    public Block getBlock() { return block; }
    public void setBlock(Block block) { this.block = block; }
    
    public User getHost() { return host; }
    public void setHost(User host) { this.host = host; }

    public int getTimeLimit() { return timeLimit; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }

    public java.time.LocalDateTime getCurrentQuestionStartTime() { return currentQuestionStartTime; }
    public void setCurrentQuestionStartTime(java.time.LocalDateTime currentQuestionStartTime) { this.currentQuestionStartTime = currentQuestionStartTime; }

    public List<RoomQuestion> getRoomQuestions() { return roomQuestions; }
    public void setRoomQuestions(List<RoomQuestion> roomQuestions) { this.roomQuestions = roomQuestions; }

    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }
}
