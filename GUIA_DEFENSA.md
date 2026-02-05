# 🎯 GUÍA DE DEFENSA - Quiz Live

## PARTE 1: PREGUNTAS SOBRE CONCURRENCIA (PSP)

---

### ❓ "¿Qué estructuras concurrentes usas en tu proyecto?"

**RESPUESTA:**
> "Uso tres estructuras principales de `java.util.concurrent`:
> 
> 1. **ConcurrentHashMap** - Para almacenar las salas activas sin bloqueos
> 2. **ExecutorService** - Un pool de hilos para procesar respuestas en paralelo
> 3. **ScheduledExecutorService** - Para los temporizadores de cada pregunta
> 4. **AtomicBoolean** - Para controlar si la pregunta está abierta o cerrada de forma atómica"

**CÓDIGO DE EJEMPLO:**
```java
// QuizEngine.java
private ConcurrentHashMap<String, ActiveRoom> activeRooms = new ConcurrentHashMap<>();
private ExecutorService answerThreadPool = Executors.newCachedThreadPool();
private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
```

---

### ❓ "¿Por qué usas ConcurrentHashMap y no HashMap normal?"

**RESPUESTA:**
> "Porque **HashMap no es thread-safe**. Si dos hilos intentan escribir a la vez, puede corromperse y perder datos.
> 
> **ConcurrentHashMap** permite:
> - Lecturas simultáneas sin bloqueo
> - Escrituras en segmentos diferentes en paralelo
> - Operaciones atómicas como `putIfAbsent()` y `merge()`
> 
> Ejemplo: si dos jugadores responden a la vez, sus respuestas se procesan sin interferirse."

---

### ❓ "¿Cómo funciona el temporizador de cada pregunta?"

**RESPUESTA:**
> "Uso un **ScheduledExecutorService** que programa una tarea para ejecutarse después de X segundos:
> 
> 1. Al empezar una pregunta, llamo a `scheduler.schedule()`
> 2. La tarea se ejecuta en un **hilo separado** cuando pasan los segundos
> 3. Cuando se ejecuta, cambia `questionOpen` a `false`
> 4. Cualquier respuesta que llegue después será rechazada"

**CÓDIGO:**
```java
ScheduledFuture<?> task = scheduler.schedule(() -> {
    room.getQuestionOpen().set(false);  // Cierra la pregunta
    log("Tiempo agotado. Pregunta CERRADA", pin);
}, seconds, TimeUnit.SECONDS);
```

---

### ❓ "¿Qué es un ExecutorService y por qué lo usas?"

**RESPUESTA:**
> "Un **ExecutorService** es un pool de hilos gestionado por Java. En vez de crear un hilo nuevo por cada tarea (costoso), reutilizo hilos del pool.
> 
> Yo uso `Executors.newCachedThreadPool()` que:
> - Crea hilos nuevos cuando los necesita
> - Reutiliza hilos que terminaron
> - Escala automáticamente según la demanda
> 
> Esto permite procesar **muchas respuestas simultáneas** sin bloquear el servidor."

---

### ❓ "¿Cómo evitas que un jugador responda dos veces?"

**RESPUESTA:**
> "Uso un **ConcurrentHashMap.newKeySet()** que es un Set thread-safe:
> 
> ```java
> Set<Long> playersWhoAnswered = ConcurrentHashMap.newKeySet();
> 
> // Cuando un jugador responde:
> if (!playersWhoAnswered.add(playerId)) {
>     // add() devuelve FALSE si ya existía
>     log("Respuesta RECHAZADA (duplicada)");
>     return;
> }
> ```
> 
> La operación `add()` es **atómica**: verifica y añade en un solo paso, imposible que dos hilos pasen a la vez."

---

### ❓ "¿Qué es AtomicBoolean y por qué lo usas?"

**RESPUESTA:**
> "Es un wrapper de boolean que garantiza **operaciones atómicas**.
> 
> Si uso `boolean normal` y dos hilos lo leen/escriben a la vez, puede haber inconsistencias.
> 
> Con **AtomicBoolean**:
> - `get()` lee el valor de forma segura
> - `set()` escribe de forma segura
> - `compareAndSet()` hace ambas cosas en una operación
> 
> Lo uso para saber si la pregunta está abierta o cerrada."

---

### ❓ "¿Qué pasa si llegan 100 respuestas a la vez?"

**RESPUESTA:**
> "El sistema las maneja sin problemas gracias al pool de hilos:
> 
> 1. Cada respuesta entra por HTTP (hilo del servidor Tomcat)
> 2. Se envía al `answerThreadPool` con `submit()`
> 3. El pool **asigna un hilo libre** a cada tarea
> 4. Todas se procesan **en paralelo**
> 5. Las estructuras thread-safe evitan colisiones
> 
> El servidor no se bloquea porque el procesamiento es **asíncrono**."

---

### ❓ "¿Cómo sé qué hilo está ejecutando cada cosa?"

**RESPUESTA:**
> "Tengo un método de logging que muestra el nombre del hilo:
> 
> ```java
> private void log(String msg, String pin) {
>     System.out.println(\"[Room \" + pin + \"] [\" + Thread.currentThread().getName() + \"] \" + msg);
> }
> ```
> 
> En la consola se ve:
> ```
> [Room 48291] [pool-1-thread-1] Respuesta CORRECTA - Jugador 5
> [Room 48291] [pool-1-thread-2] Respuesta INCORRECTA - Jugador 7
> [Room 48291] [pool-2-thread-1] Tiempo agotado. Pregunta CERRADA
> ```
> 
> Puedo ver que cada respuesta la procesa un hilo diferente."

---

### ❓ "¿Pueden existir varias salas de juego a la vez?"

**RESPUESTA:**
> "Sí, cada sala es **completamente independiente**:
> 
> - Se guarda en el `ConcurrentHashMap<String, ActiveRoom>` con su PIN como clave
> - Cada sala tiene su propio temporizador
> - Cada sala tiene su propio set de jugadores y puntuaciones
> - Las acciones en la sala A no afectan a la sala B
> 
> Esto es posible porque el `ConcurrentHashMap` permite acceso paralelo a diferentes claves."

---

---

## PARTE 2: PREGUNTAS SOBRE SPRING BOOT

---

### ❓ "¿Qué es Spring Boot y por qué lo usas?"

**RESPUESTA:**
> "Spring Boot es un framework de Java que simplifica crear aplicaciones web:
> 
> - **Auto-configuración**: Configura todo automáticamente
> - **Servidor embebido**: Trae Tomcat incluido, no necesito instalarlo
> - **Inyección de dependencias**: Los componentes se conectan solos
> - **Spring Data JPA**: Acceso a base de datos sin escribir SQL
> 
> Básicamente, me ahorra escribir mucho código repetitivo."

---

### ❓ "¿Qué son las anotaciones @Controller, @Service, @Repository?"

**RESPUESTA:**
> "Son **estereotipos** que indican la capa de cada clase:
> 
> | Anotación | Capa | Responsabilidad |
> |-----------|------|-----------------|
> | `@Controller` | Presentación | Recibe peticiones HTTP, devuelve vistas |
> | `@Service` | Negocio | Contiene la lógica del juego |
> | `@Repository` | Datos | Accede a la base de datos |
> 
> Spring las detecta automáticamente y las gestiona."

---

### ❓ "¿Qué es @Autowired?"

**RESPUESTA:**
> "Es **inyección de dependencias**. En vez de crear objetos manualmente con `new`, Spring los inyecta:
> 
> ```java
> @Controller
> public class GameController {
>     @Autowired
>     private GameService gameService;  // Spring lo inyecta automáticamente
> }
> ```
> 
> Ventajas:
> - No me preocupo de crear instancias
> - Spring gestiona el ciclo de vida
> - Facilita el testing (puedo inyectar mocks)"

---

### ❓ "¿Cómo funciona JPA y los Repository?"

**RESPUESTA:**
> "**JPA** (Java Persistence API) mapea clases Java a tablas de base de datos:
> 
> ```java
> @Entity
> public class Player {
>     @Id @GeneratedValue
>     private Long id;
>     private String nickname;
>     private int score;
> }
> ```
> 
> Los **Repository** generan consultas automáticamente:
> 
> ```java
> public interface PlayerRepository extends JpaRepository<Player, Long> {
>     // Spring genera automáticamente:
>     // findById(), save(), delete(), findAll()...
> }
> ```
> 
> No escribo SQL, Spring lo genera por mí."

---

### ❓ "¿Qué es MVC y cómo lo aplicas?"

**RESPUESTA:**
> "**Model-View-Controller** es un patrón de arquitectura:
> 
> - **Model**: Las clases `@Entity` (Player, GameRoom, Question...)
> - **View**: Las plantillas HTML con Thymeleaf
> - **Controller**: Las clases `@Controller` que conectan ambos
> 
> Flujo:
> 1. Usuario hace click → HTTP Request
> 2. Controller recibe y llama al Service
> 3. Service ejecuta lógica y accede a Repository
> 4. Controller pone datos en Model
> 5. Thymeleaf renderiza la View con esos datos"

---

### ❓ "¿Qué es Thymeleaf?"

**RESPUESTA:**
> "Es un **motor de plantillas** para generar HTML dinámico:
> 
> ```html
> <p th:text=\"${player.nickname}\">Nombre</p>
> <div th:each=\"jugador : ${players}\">
>     <span th:text=\"${jugador.score}\"></span>
> </div>
> <span th:if=\"${player.score > 100}\">¡Ganador!</span>
> ```
> 
> - `th:text` → Inserta texto dinámico
> - `th:each` → Itera sobre listas
> - `th:if` → Condicionales
> - `th:href` → URLs dinámicas"

---

### ❓ "¿Cómo se conecta a la base de datos?"

**RESPUESTA:**
> "En `application.properties`:
> 
> ```properties
> spring.datasource.url=jdbc:mysql://localhost:3306/quizlive
> spring.datasource.username=root
> spring.datasource.password=
> spring.jpa.hibernate.ddl-auto=update
> ```
> 
> - `ddl-auto=update` → Crea/actualiza tablas automáticamente
> - Spring Boot configura el pool de conexiones solo
> - JPA traduce mis objetos Java a SQL"

---

### ❓ "¿Cuál es la estructura de tu proyecto?"

**RESPUESTA:**
> ```
> src/main/java/com/quizlive/
> ├── controller/    → GameController, BlockController...
> ├── service/       → GameService (lógica del juego)
> ├── repository/    → PlayerRepository, QuestionRepository...
> ├── model/         → Player, GameRoom, Question, Answer...
> ├── engine/        → QuizEngine, ActiveRoom (concurrencia)
> └── dto/           → GameConfigDTO
> 
> src/main/resources/
> ├── templates/     → HTML con Thymeleaf
> ├── static/css/    → Estilos
> └── application.properties
> ```

---

---

## PARTE 3: FLUJO COMPLETO DEL JUEGO

---

### ❓ "Explícame cómo funciona una partida de principio a fin"

**RESPUESTA:**

> **1. CREAR SALA**
> - El profe selecciona un bloque de preguntas
> - Se crea un `GameRoom` con un PIN aleatorio
> - Se inicializa en el `QuizEngine` con `initRoom(pin, roomId)`
> 
> **2. JUGADORES SE UNEN**
> - Entran a `/play` e introducen el PIN
> - Se crea un `Player` y se asocia al `GameRoom`
> - Se les redirige a la sala de espera
> 
> **3. EMPIEZA EL JUEGO**
> - El profe pulsa "Iniciar"
> - `room.setStatus("PLAYING")`
> - Se inicia el timer con `startQuestionTimer(pin, seconds)`
> - Los jugadores ven la primera pregunta
> 
> **4. RESPONDER PREGUNTA**
> - Jugador pulsa una opción
> - Se llama a `GameService.submitAnswer(playerId, optionIndex)`
> - Se envía al `QuizEngine.processAnswerAsync()` → hilo del pool
> - Se verifica: ¿tiempo OK? ¿no duplicada? ¿correcta?
> - Se guarda en BD y se actualiza puntuación
> - Jugador va a pantalla de espera "¡Correcto!/¡Incorrecto!"
> 
> **5. SIGUIENTE PREGUNTA**
> - El profe pulsa "Siguiente"
> - Se incrementa `currentQuestionIndex`
> - Se reinicia el timer
> - Los jugadores salen de la espera y ven la nueva pregunta
> 
> **6. FIN DEL JUEGO**
> - Se acaban las preguntas o el profe termina
> - `room.setStatus("FINISHED")`
> - Se muestra el ranking con el podio

---

---

## PARTE 4: CÓDIGO CLAVE A RECORDAR

---

### 🔧 QuizEngine.java - Procesar respuesta async

```java
public void processAnswerAsync(String pin, Long playerId, boolean isCorrect, Runnable persistCallback) {
    ActiveRoom room = activeRooms.get(pin);
    if (room == null) return;

    answerThreadPool.submit(() -> {
        // 1. ¿Tiempo OK?
        if (!room.getQuestionOpen().get()) {
            log("RECHAZADA (Tiempo agotado)", pin);
            return;
        }
        
        // 2. ¿Ya respondió?
        if (!room.getPlayersWhoAnsweredCurrentQuestion().add(playerId)) {
            log("RECHAZADA (Duplicada)", pin);
            return;
        }
        
        // 3. Actualizar puntuación
        if (isCorrect) {
            room.getScores().merge(playerId, 1, Integer::sum);
        }
        
        // 4. Guardar en BD
        if (persistCallback != null) {
            persistCallback.run();
        }
    });
}
```

---

### 🔧 GameService.java - Submit Answer

```java
public boolean submitAnswer(Long playerId, int optionIndex) {
    Player player = playerRepository.findById(playerId).orElse(null);
    GameRoom room = player.getGameRoom();
    
    RoomQuestion currentRQ = getCurrentRoomQuestion(room);
    Question q = currentRQ.getQuestion();
    
    boolean isCorrect = (q.getCorrectOptionIndex() == optionIndex);
    
    quizEngine.processAnswerAsync(room.getPin(), playerId, isCorrect, () -> {
        saveAnswerToDb(player, currentRQ, optionIndex, isCorrect);
    });
    
    return isCorrect;
}
```

---

### 🔧 GameController.java - Mostrar juego

```java
@GetMapping("/play/game/{playerId}")
public String showPlayerGame(@PathVariable Long playerId, Model model) {
    Player player = gameService.getPlayerById(playerId);
    GameRoom room = player.getGameRoom();
    
    // ¿Juego terminado?
    if ("FINISHED".equals(room.getStatus())) {
        model.addAttribute("player", player);
        model.addAttribute("position", gameService.getPlayerPosition(player));
        return "player/finished";
    }
    
    // ¿Ya respondió esta pregunta?
    if (gameService.hasPlayerAnsweredCurrentQuestion(player)) {
        model.addAttribute("isCorrect", gameService.wasPlayerAnswerCorrect(player));
        return "player/answered";
    }
    
    // Mostrar pregunta actual
    model.addAttribute("currentQuestion", currentRQ.getQuestion());
    return "player/game";
}
```

---

---

## PARTE 5: PALABRAS CLAVE PARA LA DEFENSA

| Concepto | Definición rápida |
|----------|-------------------|
| **Thread-safe** | Seguro para usar desde múltiples hilos |
| **Atómico** | Operación indivisible, no puede interrumpirse |
| **Pool de hilos** | Conjunto de hilos reutilizables |
| **Asíncrono** | No bloquea, se ejecuta en segundo plano |
| **Inyección de dependencias** | Spring crea y conecta objetos automáticamente |
| **ORM** | Mapeo objeto-relacional (JPA/Hibernate) |
| **MVC** | Model-View-Controller, patrón de arquitectura |
| **DTO** | Data Transfer Object, para pasar datos entre capas |

---

## 📋 CHECKLIST PRE-DEFENSA

- [ ] Sé explicar qué hace cada clase del paquete `engine/`
- [ ] Puedo dibujar el diagrama de flujo de una respuesta
- [ ] Entiendo por qué uso `ConcurrentHashMap` y no `HashMap`
- [ ] Sé la diferencia entre `@Controller`, `@Service` y `@Repository`
- [ ] Puedo explicar el patrón MVC con mi proyecto de ejemplo
- [ ] Puedo mostrar los logs de hilos en la consola
- [ ] Sé cómo se evitan respuestas duplicadas
- [ ] Entiendo el flujo completo desde click hasta base de datos

---

**¡Mucha suerte en tu defensa! 💪🎮**
