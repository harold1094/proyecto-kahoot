package com.quizlive.dto;

import java.util.List;

public class GameConfigForm {
    private Long blockId;
    private int timeLimit; // Segundos
    private boolean randomMode;
    private int numQuestionsRandom;
    private List<Long> selectedQuestionIds; // Para el modo manual

    // Getters y Setters
    public Long getBlockId() { return blockId; }
    public void setBlockId(Long blockId) { this.blockId = blockId; }
    public int getTimeLimit() { return timeLimit; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }
    public boolean isRandomMode() { return randomMode; }
    public void setRandomMode(boolean randomMode) { this.randomMode = randomMode; }
    public int getNumQuestionsRandom() { return numQuestionsRandom; }
    public void setNumQuestionsRandom(int numQuestionsRandom) { this.numQuestionsRandom = numQuestionsRandom; }
    public List<Long> getSelectedQuestionIds() { return selectedQuestionIds; }
    public void setSelectedQuestionIds(List<Long> selectedQuestionIds) { this.selectedQuestionIds = selectedQuestionIds; }
}
