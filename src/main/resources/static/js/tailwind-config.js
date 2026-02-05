window.tailwind.config = {
    darkMode: "class",
    theme: {
        extend: {
            colors: {
                "primary": "#7f0df2",
                "background-light": "#f7f5f8",
                "background-dark": "#0a0612", // Used in Lobby
                "background-dark-alt": "#191022", // Used in Game
                "quiz-red": "#e21b3c",
                "quiz-blue": "#1368ce",
                "quiz-yellow": "#d89e00",
                "quiz-green": "#26890c",
            },
            fontFamily: {
                "display": ["Spline Sans", "sans-serif"]
            },
            borderRadius: {
                "DEFAULT": "0.5rem",
                "lg": "1rem",
                "xl": "1.5rem",
                "full": "9999px"
            },
        },
    },
};
