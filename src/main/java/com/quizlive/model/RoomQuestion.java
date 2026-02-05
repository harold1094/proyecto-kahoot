package com.quizlive.model;

import jakarta.persistence.*;

@Entity
public class RoomQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_room_id")
    private GameRoom gameRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    private int orderIndex; // Orden en el que aparecerá (1, 2, 3...)
    
    // Relación con Answer (para borrado en cascada)
    @OneToMany(mappedBy = "roomQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Answer> answers = new java.util.ArrayList<>();

    public RoomQuestion() {}

    public RoomQuestion(GameRoom gameRoom, Question question, int orderIndex) {
        this.gameRoom = gameRoom;
        this.question = question;
        this.orderIndex = orderIndex;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public GameRoom getGameRoom() { return gameRoom; }
    public void setGameRoom(GameRoom gameRoom) { this.gameRoom = gameRoom; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}
