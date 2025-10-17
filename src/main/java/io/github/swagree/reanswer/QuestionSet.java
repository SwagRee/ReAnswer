package io.github.swagree.reanswer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QuestionSet {
    private final String name;
    private final String displayName;
    private final String description;
    private final List<String> rewardCommands;
    private final List<Question> questions;
    private final Random random = new Random();
    
    public QuestionSet(String name, String displayName, String description, List<String> rewardCommands) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.rewardCommands = rewardCommands;
        this.questions = new ArrayList<>();
    }
    
    public void addQuestion(Question question) {
        questions.add(question);
    }
    
    public Question getRandomQuestion() {
        if (questions.isEmpty()) {
            return null;
        }
        return questions.get(random.nextInt(questions.size()));
    }
    
    // 执行奖励命令
    public void executeRewardCommands(String playerName) {
        for (String command : rewardCommands) {
            // 替换命令中的{player}占位符为实际玩家名
            String processedCommand = command.replace("{player}", playerName);
            // 执行命令（实际执行逻辑在管理器中实现）
        }
    }
    
    // Getter方法
    public String getName() {
        return name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<Question> getQuestions() {
        return new ArrayList<>(questions);
    }
    
    public boolean isEmpty() {
        return questions.isEmpty();
    }
}
