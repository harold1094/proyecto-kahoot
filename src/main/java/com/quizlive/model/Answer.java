package com.quizlive.model;

import jakarta.persistence.*;

@Entity
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    // Relacionamos con RoomQuestion para saber a qué pregunta concreta de esta partida respondió
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_question_id")
    private RoomQuestion roomQuestion;

    private int selectedOption; // 0, 1, 2, 3
    private boolean correct;

    public Answer() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public RoomQuestion getRoomQuestion() { return roomQuestion; }
    public void setRoomQuestion(RoomQuestion roomQuestion) { this.roomQuestion = roomQuestion; }

    public int getSelectedOption() { return selectedOption; }
    public void setSelectedOption(int selectedOption) { this.selectedOption = selectedOption; }

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
}
