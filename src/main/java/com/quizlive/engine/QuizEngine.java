package com.quizlive.engine;

import org.springframework.stereotype.Service;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Motor Concurrente del Quiz.
 * Gestiona los Hilos, Temporizadores y el estado en memoria.
 */
@Service
public class QuizEngine {
    
    // Requisito A: Gestión de salas concurrentes
    // Mapa Concurrente de Salas Activas (PIN -> Sala)
    private ConcurrentHashMap<String, ActiveRoom> activeRooms = new ConcurrentHashMap<>();
    
    // Requisito C: Pool de hilos para procesar respuestas de forma concurrente
    // Usamos CachedThreadPool para que crezca según demanda si hay muchos alumnos
    private ExecutorService answerThreadPool = Executors.newCachedThreadPool();
    
    // Requisito B: Pool de hilos para temporizadores (Scheduled)
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    // Inicializar sala en memoria
    public void initRoom(String pin, Long gameRoomId) {
        activeRooms.putIfAbsent(pin, new ActiveRoom(pin, gameRoomId));
        log("Sala inicializada en memoria (Engine)", pin);
    }

    // Requisito B: Temporizador concurrente
    public void startQuestionTimer(String pin, int seconds) {
        ActiveRoom room = activeRooms.get(pin);
        if (room != null) {
            room.resetForNewQuestion();
            log("Pregunta abierta. Timer iniciado: " + seconds + "s", pin);
            
            // Cancelar timer anterior si existe
            if(room.getTimerTask() != null && !room.getTimerTask().isDone()) {
                room.getTimerTask().cancel(false);
            }

            // Tarea del Timer
            ScheduledFuture<?> task = scheduler.schedule(() -> {
                // Cierre automático
                room.getQuestionOpen().set(false);
                log("Temporizador Finalizado. Pregunta CERRADA automaticamente.", pin);
            }, seconds, TimeUnit.SECONDS);
            
            room.setTimerTask(task);
        }
    }

    // Requisito C: Procesamiento concurrente de respuestas
    // Recibe los datos y lo manda a un hilo aparte
    public void processAnswerAsync(String pin, Long playerId, boolean isCorrect, Runnable persistCallback) {
        ActiveRoom room = activeRooms.get(pin);
        if (room == null) return;

        // Enviamos tarea (Runnable) al Pool de Hilos
        answerThreadPool.submit(() -> {
            try {
                // Requisito E: Logs mostrando nombre del hilo y acción
                
                // 1. Chequear si la pregunta sigue abierta (AtomicBoolean - Thread Safe)
                if (!room.getQuestionOpen().get()) {
                    log("Respuesta RECHAZADA (Tiempo agotado) - Jugador " + playerId, pin);
                    return;
                }
                
                // 2. Chequear duplicados (Set Concurrente)
                // add devuelve false si ya existía
                if (!room.getPlayersWhoAnsweredCurrentQuestion().add(playerId)) {
                    log("Respuesta RECHAZADA (Duplicada) - Jugador " + playerId, pin);
                    return;
                }
                
                // 3. Actualizar Puntuación en Memoria (Requisito D: Consistencia)
                if (isCorrect) {
                     // merge es atómico en ConcurrentHashMap
                     room.getScores().merge(playerId, 1, Integer::sum);
                     log("Respuesta CORRECTA (+1). Total: " + room.getScores().get(playerId) + " - Jugador " + playerId, pin);
                } else {
                     log("Respuesta INCORRECTA - Jugador " + playerId, pin);
                }
                
                // 4. Persistir en DB (Callback al servicio Spring)
                // Hacemos esto dentro del hilo para no bloquear el hilo principal, 
                // pero cuidado con la transacción JPA, normalmente se hace aquí si se pasa el contexto
                if (persistCallback != null) {
                    persistCallback.run(); 
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    // Helper LOGS Requisito E
    private void log(String msg, String pin) {
        // [Room PIN] [Thread-Name] Mensaje
        System.out.println("[Room " + pin + "] [" + Thread.currentThread().getName() + "] " + msg);
    }
}
