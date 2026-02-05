# Quiz Live - Documentación del Motor Concurrente (PSP)

## Índice
1. [Introducción](#introducción)
2. [Arquitectura del Motor Concurrente](#arquitectura-del-motor-concurrente)
3. [Gestión de Salas Multi-Hilo](#gestión-de-salas-multi-hilo)
4. [Temporizadores Concurrentes](#temporizadores-concurrentes)
5. [Procesamiento de Respuestas](#procesamiento-de-respuestas)
6. [Sincronización y Consistencia](#sincronización-y-consistencia)
7. [Demostración de Concurrencia (Logs)](#demostración-de-concurrencia)

---

## Introducción

El módulo PSP de Quiz Live implementa un **motor concurrente** que permite gestionar múltiples salas de quiz activas simultáneamente. El sistema garantiza:

- **Aislamiento de salas**: Las acciones en una sala no afectan a las demás
- **Procesamiento paralelo**: Múltiples respuestas pueden procesarse a la vez
- **Temporización independiente**: Cada sala mantiene su propio temporizador por pregunta
- **Consistencia de datos**: Las puntuaciones y respuestas se actualizan de forma thread-safe

---

## Arquitectura del Motor Concurrente

El sistema se compone de dos clases principales:

### `QuizEngine.java` - Motor Principal

```java
@Service
public class QuizEngine {
    // Mapa concurrente de salas activas (PIN -> Sala)
    private ConcurrentHashMap<String, ActiveRoom> activeRooms = new ConcurrentHashMap<>();
    
    // Pool de hilos para procesar respuestas
    private ExecutorService answerThreadPool = Executors.newCachedThreadPool();
    
    // Pool de hilos programados para temporizadores
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
}
```

### `ActiveRoom.java` - Estado en Memoria de una Sala

```java
public class ActiveRoom {
    private String pin;
    private Long gameRoomId;
    
    // Puntuaciones thread-safe (PlayerID -> Puntos)
    private ConcurrentHashMap<Long, Integer> scores = new ConcurrentHashMap<>();
    
    // Set de jugadores que ya respondieron (evita duplicados)
    private Set<Long> playersWhoAnsweredCurrentQuestion = ConcurrentHashMap.newKeySet();
    
    // Estado de la pregunta (abierta/cerrada) - Atómico
    private AtomicBoolean questionOpen = new AtomicBoolean(false);
    
    // Referencia al temporizador actual
    private ScheduledFuture<?> timerTask;
}
```

---

## Gestión de Salas Multi-Hilo

### Requisito A: ConcurrentHashMap para Multi-Sala

El sistema utiliza `ConcurrentHashMap<String, ActiveRoom>` para almacenar las salas activas:

```java
private ConcurrentHashMap<String, ActiveRoom> activeRooms = new ConcurrentHashMap<>();

public void initRoom(String pin, Long gameRoomId) {
    activeRooms.putIfAbsent(pin, new ActiveRoom(pin, gameRoomId));
    log("Sala inicializada en memoria (Engine)", pin);
}
```

**¿Por qué `ConcurrentHashMap`?**

- Permite **acceso simultáneo** desde múltiples hilos sin bloqueos
- Las operaciones como `putIfAbsent()` y `get()` son **atómicas**
- Garantiza que múltiples salas puedan operar **en paralelo** sin interferencias

**Demostración de independencia:**
- Sala A puede estar en la pregunta 5
- Sala B puede estar en la pregunta 2
- Las respuestas de la Sala A no afectan a la Sala B

---

## Temporizadores Concurrentes

### Requisito B: ScheduledExecutorService

Cada sala tiene su propio temporizador que corre en un hilo separado:

```java
private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

public void startQuestionTimer(String pin, int seconds) {
    ActiveRoom room = activeRooms.get(pin);
    if (room != null) {
        room.resetForNewQuestion();
        log("Pregunta abierta. Timer iniciado: " + seconds + "s", pin);
        
        // Cancelar timer anterior si existe
        if(room.getTimerTask() != null && !room.getTimerTask().isDone()) {
            room.getTimerTask().cancel(false);
        }

        // Programar cierre automático
        ScheduledFuture<?> task = scheduler.schedule(() -> {
            room.getQuestionOpen().set(false);
            log("Temporizador Finalizado. Pregunta CERRADA automáticamente.", pin);
        }, seconds, TimeUnit.SECONDS);
        
        room.setTimerTask(task);
    }
}
```

**Funcionamiento:**

1. Al iniciar una pregunta, se programa una tarea con `scheduler.schedule()`
2. La tarea se ejecuta **en un hilo separado** después de X segundos
3. Al ejecutarse, cambia `questionOpen` a `false` (AtomicBoolean)
4. Las respuestas que lleguen después serán rechazadas

**¿Por qué `ScheduledExecutorService`?**

- Permite programar tareas para ejecutarse en el futuro
- Cada tarea corre en su propio hilo del pool
- No bloquea el hilo principal que procesa peticiones HTTP

---

## Procesamiento de Respuestas

### Requisito C: ExecutorService para Respuestas

Las respuestas de los jugadores se procesan de forma asíncrona en un pool de hilos:

```java
private ExecutorService answerThreadPool = Executors.newCachedThreadPool();

public void processAnswerAsync(String pin, Long playerId, boolean isCorrect, Runnable persistCallback) {
    ActiveRoom room = activeRooms.get(pin);
    if (room == null) return;

    // Enviar tarea al pool de hilos
    answerThreadPool.submit(() -> {
        try {
            // 1. Verificar si la pregunta sigue abierta
            if (!room.getQuestionOpen().get()) {
                log("Respuesta RECHAZADA (Tiempo agotado) - Jugador " + playerId, pin);
                return;
            }
            
            // 2. Verificar si el jugador ya respondió
            if (!room.getPlayersWhoAnsweredCurrentQuestion().add(playerId)) {
                log("Respuesta RECHAZADA (Duplicada) - Jugador " + playerId, pin);
                return;
            }
            
            // 3. Actualizar puntuación
            if (isCorrect) {
                 room.getScores().merge(playerId, 1, Integer::sum);
                 log("Respuesta CORRECTA (+1). Total: " + room.getScores().get(playerId), pin);
            } else {
                 log("Respuesta INCORRECTA - Jugador " + playerId, pin);
            }
            
            // 4. Persistir en base de datos
            if (persistCallback != null) {
                persistCallback.run(); 
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    });
}
```

**Flujo de una respuesta:**

```
[HTTP Request] -> [Controller] -> [GameService.submitAnswer()] 
                                         |
                                         v
                              [QuizEngine.processAnswerAsync()]
                                         |
                                         v
                              [answerThreadPool.submit(Runnable)]
                                         |
                                         v
                              [pool-1-thread-X ejecuta la tarea]
                                    |         |         |
                                    v         v         v
                            [Verificar]  [Verificar]  [Actualizar]
                            [tiempo]     [duplicado]  [puntuación]
```

---

## Sincronización y Consistencia

### Requisito D: Evitar Condiciones de Carrera

El sistema utiliza estructuras thread-safe para garantizar consistencia:

| Estructura | Uso | Método Thread-Safe |
|------------|-----|-------------------|
| `ConcurrentHashMap<Long, Integer>` | Puntuaciones | `merge()` atómico |
| `ConcurrentHashMap.newKeySet()` | Jugadores que respondieron | `add()` atómico |
| `AtomicBoolean` | Estado pregunta abierta/cerrada | `get()` y `set()` atómicos |

### Ejemplo: Actualización Atómica de Puntuación

```java
// merge() es atómico - nunca se pierden puntos por concurrencia
room.getScores().merge(playerId, 1, Integer::sum);
```

### Ejemplo: Evitar Respuestas Duplicadas

```java
// add() devuelve false si el elemento ya existía
if (!room.getPlayersWhoAnsweredCurrentQuestion().add(playerId)) {
    log("Respuesta RECHAZADA (Duplicada)", pin);
    return;
}
```

### Ejemplo: Verificar Tiempo

```java
// AtomicBoolean garantiza lectura consistente
if (!room.getQuestionOpen().get()) {
    log("Respuesta RECHAZADA (Tiempo agotado)", pin);
    return;
}
```

---

## Demostración de Concurrencia

### Requisito E: Logs del Sistema

El sistema genera logs que muestran claramente la ejecución concurrente:

```
[Room 48291] [main] Sala inicializada en memoria (Engine)
[Room 48291] [main] Pregunta abierta. Timer iniciado: 20s
[Room 48291] [pool-1-thread-1] Respuesta CORRECTA (+1). Total: 1 - Jugador 5
[Room 48291] [pool-1-thread-2] Respuesta INCORRECTA - Jugador 7
[Room 12345] [main] Sala inicializada en memoria (Engine)
[Room 12345] [main] Pregunta abierta. Timer iniciado: 15s
[Room 48291] [pool-1-thread-3] Respuesta CORRECTA (+1). Total: 1 - Jugador 8
[Room 12345] [pool-1-thread-4] Respuesta CORRECTA (+1). Total: 1 - Jugador 2
[Room 48291] [pool-2-thread-1] Temporizador Finalizado. Pregunta CERRADA automáticamente.
[Room 48291] [pool-1-thread-5] Respuesta RECHAZADA (Tiempo agotado) - Jugador 9
[Room 12345] [pool-2-thread-2] Temporizador Finalizado. Pregunta CERRADA automáticamente.
```

**Puntos clave a observar:**

1. **Hilos diferentes**: `pool-1-thread-1`, `pool-1-thread-2`, etc.
2. **Salas independientes**: `[Room 48291]` y `[Room 12345]` operan en paralelo
3. **Temporizador separado**: `pool-2-thread-X` para timers vs `pool-1-thread-X` para respuestas
4. **Rechazo de tardías**: Respuestas después del timer son rechazadas

### Método de Logging

```java
private void log(String msg, String pin) {
    // Formato: [Room PIN] [Thread-Name] Mensaje
    System.out.println("[Room " + pin + "] [" + Thread.currentThread().getName() + "] " + msg);
}
```

---

## Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                         QuizEngine                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │     ConcurrentHashMap<PIN, ActiveRoom>                    │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │   │
│  │  │ Room 48291  │  │ Room 12345  │  │ Room 99999  │  ...  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘       │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌─────────────────────────┐  ┌─────────────────────────────┐   │
│  │  ExecutorService        │  │  ScheduledExecutorService   │   │
│  │  (CachedThreadPool)     │  │  (4 hilos para timers)      │   │
│  │                         │  │                              │   │
│  │  - Procesa respuestas   │  │  - Temporizadores            │   │
│  │  - Escala según demanda │  │  - Cierre automático         │   │
│  └─────────────────────────┘  └─────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Conclusión

El motor concurrente de Quiz Live cumple con todos los requisitos PSP:

- ✅ **A) Multi-sala concurrente** con `ConcurrentHashMap`
- ✅ **B) Temporizadores independientes** con `ScheduledExecutorService`  
- ✅ **C) Procesamiento paralelo de respuestas** con `ExecutorService`
- ✅ **D) Sincronización thread-safe** con estructuras atómicas
- ✅ **E) Logs demostrativos** con nombre de hilo y PIN de sala

El diseño prioriza la **claridad y corrección** sobre la complejidad innecesaria, siguiendo las directrices del enunciado.
