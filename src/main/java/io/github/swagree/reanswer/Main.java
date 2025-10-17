package io.github.swagree.reanswer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    public static Main plugin;
    private QuestionManager questionManager;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        questionManager = new QuestionManager(this);
        getLogger().info("QuestionPlugin 已启用！");
        
    }

    @Override
    public void onDisable() {
        getLogger().info("QuestionPlugin 已禁用！");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // 玩家回答命令
        if (cmd.getName().equalsIgnoreCase("question")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("只有玩家可以使用此命令！");
                return true;
            }
            
            Player player = (Player) sender;
            
            if (args.length == 0) {
                player.sendMessage("使用方法: /question rapid (抢答) 或 /question answer <你的答案>");
                return true;
            }
            
            // 处理抢答
            if (args[0].equalsIgnoreCase("rapid")) {
                questionManager.handleRapid(player);
                return true;
            }
            
            // 处理回答
            if (args[0].equalsIgnoreCase("answer") && args.length > 1) {
                // 拼接答案（处理空格）
                StringBuilder answer = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (i > 1) answer.append(" ");
                    answer.append(args[i]);
                }
                questionManager.handleAnswer(player, answer.toString());
                return true;
            }
            
            player.sendMessage("未知命令！使用方法: /question rapid (抢答) 或 /question answer <你的答案>");
            return true;
        }
        
        // 管理员配置命令
        if (cmd.getName().equalsIgnoreCase("qaconfig")) {
            if (!sender.hasPermission("question.admin")) {
                sender.sendMessage("你没有权限使用此命令！");
                return true;
            }
            
            if (args.length == 0) {
                sender.sendMessage("管理员命令:");
                sender.sendMessage("/qaconfig reload - 重新加载配置");
                sender.sendMessage("/qaconfig interval <秒数> - 设置问题间隔时间");
                sender.sendMessage("/qaconfig rapidmode - 切换抢答模式");
                sender.sendMessage("/qaconfig send - 立即发送一个问题");
                return true;
            }
            
            // 重新加载配置
            if (args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                questionManager.loadSettings();
                sender.sendMessage("配置已重新加载！");
                return true;
            }
            
            // 设置时间间隔
            if (args[0].equalsIgnoreCase("interval") && args.length > 1) {
                try {
                    int interval = Integer.parseInt(args[1]);
                    if (interval > 0) {
                        questionManager.setInterval(interval);
                        sender.sendMessage("问题间隔已设置为 " + interval + " 秒！");
                    } else {
                        sender.sendMessage("间隔时间必须大于0！");
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("请输入有效的数字！");
                }
                return true;
            }
            
            // 切换抢答模式
            if (args[0].equalsIgnoreCase("rapidmode")) {
                boolean newMode = !questionManager.isRapidMode();
                questionManager.setRapidMode(newMode);
                sender.sendMessage("抢答模式已" + (newMode ? "启用" : "禁用") + "！");
                return true;
            }
            
            // 立即发送问题
            if (args[0].equalsIgnoreCase("send")) {
                questionManager.forceSendQuestion();
                sender.sendMessage("已发送一个问题！");
                return true;
            }
            
            sender.sendMessage("未知命令！输入 /qaconfig 查看帮助");
            return true;
        }
        
        return false;
    }
}
