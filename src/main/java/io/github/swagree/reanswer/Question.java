package io.github.swagree.reanswer;

public class Question {
    private final String question;
    private final String answer;
    
    public Question(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }
    
    public String getQuestion() {
        return question;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    // 检查答案是否正确（忽略大小写）
    public boolean isCorrectAnswer(String playerAnswer) {
        return answer.equalsIgnoreCase(playerAnswer.trim());
    }
}
