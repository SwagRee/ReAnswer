package io.github.swagree.reanswer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.ComponentBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestionManager {
    private final Main plugin;
    private final Map<String, QuestionSet> questionSets = new ConcurrentHashMap<>();
    private final List<String> activeSets = new ArrayList<>();
    private int interval = 300; // 默认5分钟(300秒)
    private boolean rapidMode = true; // 默认启用抢答
    private boolean questionActive = false;
    private Question currentQuestion;
    private QuestionSet currentQuestionSet;
    private Player currentWinner = null;
    private Set<Player> answeredPlayers = new HashSet<>();

    public QuestionManager(Main plugin) {
        this.plugin = plugin;
        loadSettings();
        startQuestionScheduler();
    }

    // 加载配置设置
    public void loadSettings() {
        interval = plugin.getConfig().getInt("interval", 300);
        rapidMode = plugin.getConfig().getBoolean("rapid-mode", true);
        activeSets.clear();
        activeSets.addAll(plugin.getConfig().getStringList("active-sets"));
        loadQuestionSets();
    }

    // 加载所有题库
    private void loadQuestionSets() {
        questionSets.clear();

        if (!plugin.getConfig().contains("question-sets")) {
            return;
        }

        for (String setName : plugin.getConfig().getConfigurationSection("question-sets").getKeys(false)) {
            String path = "question-sets." + setName;

            String displayName = plugin.getConfig().getString(path + ".display-name", setName);
            String description = plugin.getConfig().getString(path + ".description", "无描述");
            List<String> commands = plugin.getConfig().getStringList(path + ".commands");

            // 创建题库实例，传入奖励命令列表
            QuestionSet questionSet = new QuestionSet(setName, displayName, description, commands);

            // 添加问题
            if (plugin.getConfig().contains(path + ".questions")) {
                List<Map<?, ?>> questions = plugin.getConfig().getMapList(path + ".questions");
                for (Map<?, ?> questionMap : questions) {
                    String questionText = (String) questionMap.get("question");
                    String answerText = (String) questionMap.get("answer");

                    if (questionText != null && answerText != null) {
                        questionSet.addQuestion(new Question(questionText, answerText));
                    }
                }
            }

            questionSets.put(setName, questionSet);
        }
    }

    // 启动问题调度器
    private void startQuestionScheduler() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!questionActive && !activeSets.isEmpty()) {
                    sendRandomQuestion();
                }
            }
        }.runTaskTimer(plugin, 0, interval * 20L);
    }

    // 发送随机问题
    public void sendRandomQuestion() {
        questionActive = true;
        currentWinner = null;
        answeredPlayers.clear();
        currentQuestion = null;
        currentQuestionSet = null;

        List<String> validSets = new ArrayList<>();
        for (String setName : activeSets) {
            QuestionSet set = questionSets.get(setName);
            if (set != null && !set.isEmpty()) {
                validSets.add(setName);
            }
        }

        if (validSets.isEmpty()) {
            questionActive = false;
            return;
        }

        String randomSetName = validSets.get(new Random().nextInt(validSets.size()));
        currentQuestionSet = questionSets.get(randomSetName);
        currentQuestion = currentQuestionSet.getRandomQuestion();

        if (currentQuestion == null) {
            questionActive = false;
            return;
        }

        broadcastQuestion();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (questionActive) {
                    questionActive = false;
                    if (currentWinner == null) {
                        Bukkit.broadcastMessage(ChatColor.RED + "❌ 问题超时，无人回答正确！");
                        Bukkit.broadcastMessage(ChatColor.GRAY + "正确答案是: " + currentQuestion.getAnswer());
                    }
                }
            }
        }.runTaskLater(plugin, 600L);
    }

    // 广播问题
    private void broadcastQuestion() {
        Bukkit.broadcastMessage(ChatColor.GOLD + "\n===== 问答时间 =====");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "问题: " + currentQuestion.getQuestion());

        // 创建抢答按钮
        TextComponent rapidButton = new TextComponent();
        rapidButton.setText(ChatColor.RED + "[点击抢答]");
        rapidButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/question rapid"));
        rapidButton.setHoverEvent(new HoverEvent(
                Action.SHOW_TEXT,
                new ComponentBuilder(ChatColor.GRAY + "点击参与抢答").create()
        ));

        BaseComponent[] rapidMessage = new BaseComponent[]{rapidButton};
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(rapidMessage);
        }

        Bukkit.broadcastMessage(ChatColor.GOLD + "===================\n");
    }

    // 处理玩家抢答
    public void handleRapid(Player player) {
        if (!questionActive) {
            player.sendMessage(ChatColor.RED + "当前没有活动的问题！");
            return;
        }

        if (rapidMode && currentWinner != null) {
            player.sendMessage(ChatColor.RED + "很遗憾，" + currentWinner.getName() + "已经抢先一步！");
            return;
        }

        if (rapidMode && currentWinner == null) {
            currentWinner = player;
            Bukkit.broadcastMessage(ChatColor.GREEN + player.getName() + " 抢到了回答权！");
        }

        sendAnswerPrompt(player);
    }

    // 发送答案输入提示
    private void sendAnswerPrompt(Player player) {
        if (rapidMode && currentWinner != null && !player.equals(currentWinner)) {
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "\n请输入答案:");

        TextComponent answerButton = new TextComponent();
        answerButton.setText(ChatColor.GREEN + "[点击输入答案]");
        answerButton.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/question answer "));
        answerButton.setHoverEvent(new HoverEvent(
                Action.SHOW_TEXT,
                new ComponentBuilder(ChatColor.GRAY + "点击后输入答案并发送").create()
        ));

        BaseComponent[] answerMessage = new BaseComponent[]{answerButton};
        player.spigot().sendMessage(answerMessage);
    }

    // 处理玩家答案
    public void handleAnswer(Player player, String answer) {
        if (!questionActive) {
            player.sendMessage(ChatColor.RED + "当前没有活动的问题！");
            return;
        }

        if (rapidMode && currentWinner != null && !player.equals(currentWinner)) {
            player.sendMessage(ChatColor.RED + "只有抢答者可以回答这个问题！");
            return;
        }

        if (answeredPlayers.contains(player) && rapidMode) {
            player.sendMessage(ChatColor.RED + "你已经回答过这个问题了！");
            return;
        }

        answeredPlayers.add(player);

        if (currentQuestion.isCorrectAnswer(answer)) {
            player.sendMessage(ChatColor.GREEN + "恭喜你，回答正确！");
            Bukkit.broadcastMessage(ChatColor.GREEN + player.getName() + " 回答正确！");
            // 执行奖励命令（适配QuestionSet的方法）
            executeRewardCommands(player);
            questionActive = false;
        } else {
            player.sendMessage(ChatColor.RED + "回答错误，请再试一次！");
            if (rapidMode) {
                sendAnswerPrompt(player);
            }
        }
    }

    // 执行奖励命令（修正为适配QuestionSet的实现）
    private void executeRewardCommands(Player player) {
        if (currentQuestionSet == null) {
            return;
        }

        // 获取当前题库的奖励命令列表（通过反射获取私有字段，因为没有getter）
        try {
            // 使用反射获取QuestionSet中的rewardCommands私有字段
            java.lang.reflect.Field field = QuestionSet.class.getDeclaredField("rewardCommands");
            field.setAccessible(true);
            List<String> commands = (List<String>) field.get(currentQuestionSet);

            // 执行所有奖励命令
            for (String command : commands) {
                String processedCommand = command.replace("{player}", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果反射失败，尝试调用QuestionSet中的处理方法
            plugin.getLogger().warning("无法获取奖励命令，尝试备选方案: " + e.getMessage());
            currentQuestionSet.executeRewardCommands(player.getName());
        }
    }

    // 管理员命令：立即发送问题
    public void forceSendQuestion() {
        if (!questionActive) {
            sendRandomQuestion();
        }
    }

    // Getter和Setter
    public int getInterval() { return interval; }
    public void setInterval(int interval) {
        this.interval = interval;
        plugin.getConfig().set("interval", interval);
        plugin.saveConfig();
    }
    public boolean isRapidMode() { return rapidMode; }
    public void setRapidMode(boolean rapidMode) {
        this.rapidMode = rapidMode;
        plugin.getConfig().set("rapid-mode", rapidMode);
        plugin.saveConfig();
    }
}
