package com.quizlive.engine;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Clase que representa una sala activa en MEMORIA (RAM).
 * No es una entidad JPA. Se usa para la gestión concurrente rápida.
 */
public class ActiveRoom {
    private String pin;
    private Long gameRoomId;
    
    // Puntuaciones en memoria (PlayerID -> Puntos)
    // Requisito D: Uso de ConcurrentHashMap para puntuaciones
    private ConcurrentHashMap<Long, Integer> scores = new ConcurrentHashMap<>();
    
    // Jugadores que ya han respondido a la pregunta actual (para evitar duplicados)
    private Set<Long> playersWhoAnsweredCurrentQuestion = ConcurrentHashMap.newKeySet();
    
    // Estado concurrente de la pregunta (Abierta/Cerrada)
    private AtomicBoolean questionOpen = new AtomicBoolean(false);
    
    // Referencia al temporizador (para poder cancelarlo si hiciera falta)
    private ScheduledFuture<?> timerTask;

    public ActiveRoom(String pin, Long gameRoomId) {
        this.pin = pin;
        this.gameRoomId = gameRoomId;
    }

    public String getPin() { return pin; }
    public Long getGameRoomId() { return gameRoomId; }

    public ConcurrentHashMap<Long, Integer> getScores() { return scores; }
    
    public Set<Long> getPlayersWhoAnsweredCurrentQuestion() { return playersWhoAnsweredCurrentQuestion; }
    
    public AtomicBoolean getQuestionOpen() { return questionOpen; }
    
    public ScheduledFuture<?> getTimerTask() { return timerTask; }
    public void setTimerTask(ScheduledFuture<?> timerTask) { this.timerTask = timerTask; }
    
    public void resetForNewQuestion() {
        this.playersWhoAnsweredCurrentQuestion.clear();
        this.questionOpen.set(true);
    }
}
