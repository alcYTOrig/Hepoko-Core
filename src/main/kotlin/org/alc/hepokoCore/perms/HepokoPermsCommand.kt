package org.alc.hepokoCore.perms

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

/**
 * Enhanced command executor for HepokoPerms with detailed help and suggestions.
 */
class HepokoPermsCommand : CommandExecutor {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sendMainHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "help", "h", "?" -> {
                sendMainHelp(sender)
                return true
            }
            "group", "g" -> {
                handleGroupCommand(sender, args)
                return true
            }
            "user", "u" -> {
                handleUserCommand(sender, args)
                return true
            }
            "list", "l" -> {
                handleListCommand(sender, args)
                return true
            }
            "reload", "rl" -> {
                PermissionManager.loadCache()
                PermissionManager.loadAllUsers()
                sender.sendMessage(Component.text("✔ Конфигурация HepokoPerms перезагружена!", NamedTextColor.GREEN))
                return true
            }
            "check", "c" -> {
                if (args.size < 2) {
                    sender.sendMessage(Component.text("Использование: /hperm check <игрок>", NamedTextColor.RED))
                    return true
                }
                handleCheckCommand(sender, args[1])
                return true
            }
            else -> {
                sender.sendMessage(Component.text("Неизвестная команда. Используйте ", NamedTextColor.RED)
                    .append(Component.text("/hperm help", NamedTextColor.YELLOW))
                    .append(Component.text(" для списка команд.", NamedTextColor.RED)))
            }
        }
        return true
    }

    // ========== MAIN HELP ==========

    private fun sendMainHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("========================================", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("           HepokoPerms - Справка           ", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("========================================", NamedTextColor.GOLD))
        sender.sendMessage(Component.empty())
        
        sender.sendMessage(Component.text("📁 Управление группами:", NamedTextColor.AQUA))
        sender.sendMessage(Component.text("  /hperm group create <группа> <вес>", NamedTextColor.GREEN)
            .append(Component.text(" - Создать группу", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  /hperm group <группа> add <право>", NamedTextColor.GREEN)
            .append(Component.text(" - Добавить право группе", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  /hperm group <группа> remove <право>", NamedTextColor.RED)
            .append(Component.text(" - Удалить право у группы", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  /hperm group <группа> delete", NamedTextColor.RED)
            .append(Component.text(" - Удалить группу", NamedTextColor.GRAY)))
        
        sender.sendMessage(Component.empty())
        sender.sendMessage(Component.text("👤 Управление пользователями:", NamedTextColor.AQUA))
        sender.sendMessage(Component.text("  /hperm user <игрок> add <право>", NamedTextColor.GREEN)
            .append(Component.text(" - Добавить право игроку", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  /hperm user <игрок> remove <право>", NamedTextColor.RED)
            .append(Component.text(" - Удалить право у игрока", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  /hperm user <игрок> group set <группа>", NamedTextColor.YELLOW)
            .append(Component.text(" - Назначить группу", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  /hperm user <игрок> group list", NamedTextColor.WHITE)
            .append(Component.text(" - Показать группы игрока", NamedTextColor.GRAY)))
        
        sender.sendMessage(Component.empty())
        sender.sendMessage(Component.text("📋 Просмотр информации:", NamedTextColor.AQUA))
        sender.sendMessage(Component.text("  /hperm check <игрок>", NamedTextColor.WHITE)
            .append(Component.text(" - Проверить права игрока", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  /hperm list groups", NamedTextColor.WHITE)
            .append(Component.text(" - Список всех групп", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  /hperm list users", NamedTextColor.WHITE)
            .append(Component.text(" - Список всех пользователей", NamedTextColor.GRAY)))
        
        sender.sendMessage(Component.empty())
        sender.sendMessage(Component.text("⚙️ Настройки:", NamedTextColor.AQUA))
        sender.sendMessage(Component.text("  /hperm reload", NamedTextColor.GOLD)
            .append(Component.text(" - Перезагрузить конфигурацию", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  /hperm help", NamedTextColor.GOLD)
            .append(Component.text(" - Показать эту справку", NamedTextColor.GRAY)))
        
        sender.sendMessage(Component.empty())
        sender.sendMessage(Component.text("========================================", NamedTextColor.GOLD))
    }

    // ========== GROUP COMMANDS ==========

    private fun handleGroupCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sendGroupHelp(sender)
            return
        }

        val subCommand = args[1].lowercase()
        
        when (subCommand) {
            "create", "c" -> {
                if (args.size < 4) {
                    sender.sendMessage(Component.text("Использование: /hperm group create <группа> <вес>", NamedTextColor.RED))
                    sender.sendMessage(Component.text("Пример: /hperm group create admin 100", NamedTextColor.GRAY))
                    return
                }
                val groupName = args[2].lowercase()
                val weight = args[3].toIntOrNull()
                
                if (weight == null || weight < 0) {
                    sender.sendMessage(Component.text("❌ Укажите корректный вес группы (число >= 0)!", NamedTextColor.RED))
                    return
                }
                
                if (PermissionDatabase.groupExists(groupName)) {
                    sender.sendMessage(Component.text("❌ Группа '$groupName' уже существует!", NamedTextColor.RED))
                    return
                }
                
                PermissionDatabase.addGroup(groupName, weight)
                PermissionManager.loadCache()
                
                sender.sendMessage(Component.text("✔ Группа '", NamedTextColor.GREEN)
                    .append(Component.text(groupName, NamedTextColor.AQUA))
                    .append(Component.text("' создана с весом ", NamedTextColor.GREEN))
                    .append(Component.text(weight.toString(), NamedTextColor.YELLOW)))
            }
            
            "delete", "remove", "d", "r" -> {
                if (args.size < 3) {
                    sender.sendMessage(Component.text("Использование: /hperm group delete <группа>", NamedTextColor.RED))
                    return
                }
                val groupName = args[2].lowercase()
                
                if (groupName == "default") {
                    sender.sendMessage(Component.text("❌ Нельзя удалить группу 'default'!", NamedTextColor.RED))
                    return
                }
                
                if (!PermissionDatabase.groupExists(groupName)) {
                    sender.sendMessage(Component.text("❌ Группа '$groupName' не найдена!", NamedTextColor.RED))
                    return
                }
                
                val removed = PermissionDatabase.removeGroup(groupName)
                if (removed) {
                    PermissionManager.loadCache()
                    sender.sendMessage(Component.text("✔ Группа '", NamedTextColor.GREEN)
                        .append(Component.text(groupName, NamedTextColor.AQUA))
                        .append(Component.text("' удалена", NamedTextColor.GREEN)))
                } else {
                    sender.sendMessage(Component.text("❌ Ошибка при удалении группы", NamedTextColor.RED))
                }
            }
            
            "add", "a" -> {
                if (args.size < 4) {
                    sender.sendMessage(Component.text("Использование: /hperm group add <группа> <право>", NamedTextColor.RED))
                    sender.sendMessage(Component.text("Пример: /hperm group admin add hepokocore.*", NamedTextColor.GRAY))
                    return
                }
                val groupName = args[2].lowercase()
                val permission = args[3]
                
                if (!PermissionDatabase.groupExists(groupName)) {
                    sender.sendMessage(Component.text("❌ Группа '$groupName' не найдена!", NamedTextColor.RED))
                    return
                }
                
                PermissionDatabase.addGroupPermission(groupName, permission, true)
                PermissionManager.loadCache()
                
                sender.sendMessage(Component.text("✔ Право '", NamedTextColor.GREEN)
                    .append(Component.text(permission, NamedTextColor.YELLOW))
                    .append(Component.text("' добавлено группе '", NamedTextColor.GREEN))
                    .append(Component.text(groupName, NamedTextColor.AQUA)))
            }
            
            "remove", "rm" -> {
                if (args.size < 4) {
                    sender.sendMessage(Component.text("Использование: /hperm group remove <группа> <право>", NamedTextColor.RED))
                    return
                }
                val groupName = args[2].lowercase()
                val permission = args[3]
                
                if (!PermissionDatabase.groupExists(groupName)) {
                    sender.sendMessage(Component.text("❌ Группа '$groupName' не найдена!", NamedTextColor.RED))
                    return
                }
                
                val removed = PermissionDatabase.removeGroupPermission(groupName, permission)
                if (removed) {
                    PermissionManager.loadCache()
                    sender.sendMessage(Component.text("✔ Право '", NamedTextColor.GREEN)
                        .append(Component.text(permission, NamedTextColor.YELLOW))
                        .append(Component.text("' удалено у группы '", NamedTextColor.GREEN))
                        .append(Component.text(groupName, NamedTextColor.AQUA)))
                } else {
                    sender.sendMessage(Component.text("❌ Право не найдено у этой группы", NamedTextColor.RED))
                }
            }
            
            "list", "l" -> {
                if (args.size < 3) {
                    sender.sendMessage(Component.text("Использование: /hperm group list <группа>", NamedTextColor.RED))
                    return
                }
                val groupName = args[2].lowercase()
                
                if (!PermissionDatabase.groupExists(groupName)) {
                    sender.sendMessage(Component.text("❌ Группа '$groupName' не найдена!", NamedTextColor.RED))
                    return
                }
                
                val permissions = PermissionDatabase.getGroupPermissions(groupName)
                
                sender.sendMessage(Component.text("=== Права группы $groupName ===", NamedTextColor.GOLD))
                if (permissions.isEmpty()) {
                    sender.sendMessage(Component.text("У этой группы нет прав", NamedTextColor.GRAY))
                } else {
                    permissions.forEach { perm ->
                        sender.sendMessage(Component.text("  • ", NamedTextColor.GREEN)
                            .append(Component.text(perm, NamedTextColor.WHITE)))
                    }
                }
            }
            
            "info", "i" -> {
                if (args.size < 3) {
                    sender.sendMessage(Component.text("Использование: /hperm group info <группа>", NamedTextColor.RED))
                    return
                }
                val groupName = args[2].lowercase()
                
                if (!PermissionDatabase.groupExists(groupName)) {
                    sender.sendMessage(Component.text("❌ Группа '$groupName' не найдена!", NamedTextColor.RED))
                    return
                }
                
                val group = PermissionDatabase.getGroup(groupName)
                if (group == null) {
                    sender.sendMessage(Component.text("❌ Не удалось загрузить информацию о группе", NamedTextColor.RED))
                    return
                }
                
                sender.sendMessage(Component.text("=== Информация о группе $groupName ===", NamedTextColor.GOLD))
                sender.sendMessage(Component.text("Вес: ", NamedTextColor.AQUA)
                    .append(Component.text(group.weight.toString(), NamedTextColor.YELLOW)))
                sender.sendMessage(Component.text("Права: ", NamedTextColor.AQUA)
                    .append(Component.text(group.permissions.size.toString(), NamedTextColor.YELLOW)))
            }
            
            else -> {
                sendGroupHelp(sender)
            }
        }
    }

    private fun sendGroupHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("=== Управление группами ===", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("Использование: /hperm group <действие> [аргументы]", NamedTextColor.AQUA))
        sender.sendMessage(Component.empty())
        sender.sendMessage(Component.text("Действия:", NamedTextColor.WHITE))
        sender.sendMessage(Component.text("  create <группа> <вес>", NamedTextColor.GREEN) 
            .append(Component.text(" - Создать новую группу", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  delete <группа>", NamedTextColor.RED) 
            .append(Component.text(" - Удалить группу", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  add <группа> <право>", NamedTextColor.GREEN) 
            .append(Component.text(" - Добавить право", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  remove <группа> <право>", NamedTextColor.RED) 
            .append(Component.text(" - Удалить право", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  list <группа>", NamedTextColor.WHITE) 
            .append(Component.text(" - Список прав группы", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  info <группа>", NamedTextColor.WHITE) 
            .append(Component.text(" - Информация о группе", NamedTextColor.GRAY)))
    }

    // ========== USER COMMANDS ==========

    private fun handleUserCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sendUserHelp(sender)
            return
        }

        val subCommand = args[1].lowercase()
        
        when (subCommand) {
            "add", "a" -> {
                if (args.size < 4) {
                    sender.sendMessage(Component.text("Использование: /hperm user add <игрок> <право>", NamedTextColor.RED))
                    sender.sendMessage(Component.text("Пример: /hperm user Steve build", NamedTextColor.GRAY))
                    return
                }
                val userName = args[2]
                val permission = args[3]
                
                val player = Bukkit.getPlayer(userName)
                val uuid = player?.uniqueId?.toString()
                
                if (uuid == null) {
                    sender.sendMessage(Component.text("⚠ Игрок ", NamedTextColor.YELLOW)
                        .append(Component.text(userName, NamedTextColor.WHITE))
                        .append(Component.text(" оффлайн. Индивидуальные права работают только для онлайн-игроков.", NamedTextColor.YELLOW)))
                    return
                }
                
                PermissionDatabase.addUserPermission(uuid, permission, true)
                
                sender.sendMessage(Component.text("✔ Право '", NamedTextColor.GREEN)
                    .append(Component.text(permission, NamedTextColor.YELLOW))
                    .append(Component.text("' выдано игроку '", NamedTextColor.GREEN))
                    .append(Component.text(userName, NamedTextColor.AQUA)))
                
                player.sendMessage(Component.text("Вам выдано право: ", NamedTextColor.GREEN)
                    .append(Component.text(permission, NamedTextColor.YELLOW)))
            }
            
            "remove", "rm" -> {
                if (args.size < 4) {
                    sender.sendMessage(Component.text("Использование: /hperm user remove <игрок> <право>", NamedTextColor.RED))
                    return
                }
                val userName = args[2]
                val permission = args[3]
                
                val player = Bukkit.getPlayer(userName)
                val uuid = player?.uniqueId?.toString()
                
                if (uuid == null) {
                    sender.sendMessage(Component.text("⚠ Игрок ", NamedTextColor.YELLOW)
                        .append(Component.text(userName, NamedTextColor.WHITE))
                        .append(Component.text(" оффлайн.", NamedTextColor.YELLOW)))
                    return
                }
                
                val removed = PermissionDatabase.removeUserPermission(uuid, permission)
                if (removed) {
                    sender.sendMessage(Component.text("✔ Право '", NamedTextColor.GREEN)
                        .append(Component.text(permission, NamedTextColor.YELLOW))
                        .append(Component.text("' удалено у игрока '", NamedTextColor.GREEN))
                        .append(Component.text(userName, NamedTextColor.AQUA)))
                    
                    player.sendMessage(Component.text("У вас удалено право: ", NamedTextColor.RED)
                        .append(Component.text(permission, NamedTextColor.YELLOW)))
                } else {
                    sender.sendMessage(Component.text("❌ Право не найдено у этого игрока", NamedTextColor.RED))
                }
            }
            
            "group" -> {
                if (args.size < 4) {
                    sendUserHelp(sender)
                    return
                }
                val userName = args[2]
                val groupSubCommand = args[3].lowercase()
                
                when (groupSubCommand) {
                    "set", "s" -> {
                        if (args.size < 5) {
                            sender.sendMessage(Component.text("Использование: /hperm user group set <игрок> <группа>", NamedTextColor.RED))
                            return
                        }
                        val groupName = args[4]
                        val player = Bukkit.getPlayer(userName)
                        val uuid = player?.uniqueId
                        
                        if (uuid == null) {
                            sender.sendMessage(Component.text("⚠ Игрок ", NamedTextColor.YELLOW)
                                .append(Component.text(userName, NamedTextColor.WHITE))
                                .append(Component.text(" оффлайн. Используйте онлайн игрока.", NamedTextColor.YELLOW)))
                            return
                        }
                        
                        PermissionManager.setUserGroup(uuid, groupName)
                        sender.sendMessage(Component.text("✔ Игрок '", NamedTextColor.GREEN)
                            .append(Component.text(userName, NamedTextColor.AQUA))
                            .append(Component.text("' добавлен в группу '", NamedTextColor.GREEN))
                            .append(Component.text(groupName, NamedTextColor.YELLOW)))
                        
                        player.sendMessage(Component.text("Вы добавлены в группу: ", NamedTextColor.GREEN)
                            .append(Component.text(groupName, NamedTextColor.YELLOW)))
                    }
                    
                    "list", "l" -> {
                        val player = Bukkit.getPlayer(userName)
                        val uuid = player?.uniqueId
                        
                        if (uuid == null) {
                            sender.sendMessage(Component.text("⚠ Игрок ", NamedTextColor.YELLOW)
                                .append(Component.text(userName, NamedTextColor.WHITE))
                                .append(Component.text(" оффлайн. Показываю сохранённые группы.", NamedTextColor.YELLOW)))
                        }
                        
                        val groups = PermissionManager.getUserGroups(uuid ?: return)
                        sender.sendMessage(Component.text("Группы игрока ", NamedTextColor.AQUA)
                            .append(Component.text(userName, NamedTextColor.WHITE))
                            .append(Component.text(": ", NamedTextColor.AQUA))
                            .append(Component.text(groups.joinToString(", "), NamedTextColor.YELLOW)))
                    }
                    
                    else -> {
                        sender.sendMessage(Component.text("Неизвестное действие для группы. Используйте 'set' или 'list'", NamedTextColor.RED))
                    }
                }
            }
            
            else -> {
                sendUserHelp(sender)
            }
        }
    }

    private fun sendUserHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("=== Управление пользователями ===", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("Использование: /hperm user <действие> [аргументы]", NamedTextColor.AQUA))
        sender.sendMessage(Component.empty())
        sender.sendMessage(Component.text("Действия:", NamedTextColor.WHITE))
        sender.sendMessage(Component.text("  add <игрок> <право>", NamedTextColor.GREEN) 
            .append(Component.text(" - Добавить право игроку", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  remove <игрок> <право>", NamedTextColor.RED) 
            .append(Component.text(" - Удалить право у игрока", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  group set <игрок> <группа>", NamedTextColor.YELLOW) 
            .append(Component.text(" - Назначить группу", NamedTextColor.GRAY)))
        sender.sendMessage(Component.text("  group list <игрок>", NamedTextColor.WHITE) 
            .append(Component.text(" - Показать группы игрока", NamedTextColor.GRAY)))
    }

    // ========== LIST COMMANDS ==========

    private fun handleListCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(Component.text("Использование: /hperm list <groups|users>", NamedTextColor.RED))
            return
        }

        when (args[1].lowercase()) {
            "groups", "g" -> {
                val groups = PermissionManager.getAllGroups()
                if (groups.isEmpty()) {
                    sender.sendMessage(Component.text("Группы не найдены.", NamedTextColor.RED))
                    return
                }
                
                sender.sendMessage(Component.text("=== Список групп ===", NamedTextColor.GOLD))
                groups.sortedByDescending { it.weight }.forEach { group ->
                    sender.sendMessage(
                        Component.text()
                            .append(Component.text("  • ", NamedTextColor.GRAY))
                            .append(Component.text(group.name, NamedTextColor.AQUA))
                            .append(Component.text(" (вес: ", NamedTextColor.WHITE))
                            .append(Component.text(group.weight.toString(), NamedTextColor.YELLOW))
                            .append(Component.text(")", NamedTextColor.WHITE))
                            .build()
                    )
                }
                sender.sendMessage(Component.text("Всего: ", NamedTextColor.WHITE)
                    .append(Component.text(groups.size.toString(), NamedTextColor.YELLOW)))
            }
            
            "users", "u" -> {
                val users = PermissionDatabase.getAllUsers()
                if (users.isEmpty()) {
                    sender.sendMessage(Component.text("Пользователи не найдены.", NamedTextColor.RED))
                    return
                }
                
                sender.sendMessage(Component.text("=== Список пользователей ===", NamedTextColor.GOLD))
                users.forEach { user ->
                    sender.sendMessage(
                        Component.text()
                            .append(Component.text("  • ", NamedTextColor.GRAY))
                            .append(Component.text(user.name, NamedTextColor.AQUA))
                            .append(Component.text(" (UUID: ", NamedTextColor.WHITE))
                            .append(Component.text(user.uuid.toString().take(8), NamedTextColor.YELLOW))
                            .append(Component.text("...)", NamedTextColor.WHITE))
                            .build()
                    )
                }
                sender.sendMessage(Component.text("Всего: ", NamedTextColor.WHITE)
                    .append(Component.text(users.size.toString(), NamedTextColor.YELLOW)))
            }
            
            else -> {
                sender.sendMessage(Component.text("Неизвестная цель. Используйте 'groups' или 'users'", NamedTextColor.RED))
            }
        }
    }

    // ========== CHECK COMMAND ==========

    private fun handleCheckCommand(sender: CommandSender, playerName: String) {
        val player = Bukkit.getPlayer(playerName)
        val uuidObj = player?.uniqueId
        
        if (uuidObj == null) {
            sender.sendMessage(Component.text("⚠ Игрок ", NamedTextColor.YELLOW)
                .append(Component.text(playerName, NamedTextColor.WHITE))
                .append(Component.text(" оффлайн или не существует.", NamedTextColor.YELLOW)))
            return
        }
        
        val permissions = PermissionDatabase.getUserPermissions(uuidObj.toString())
        val groups = PermissionManager.getUserGroups(uuidObj)
        
        sender.sendMessage(Component.text("=== Права игрока $playerName ===", NamedTextColor.GOLD))
        sender.sendMessage(Component.text("Группы: ", NamedTextColor.AQUA)
            .append(Component.text(groups.joinToString(", "), NamedTextColor.YELLOW)))
        
        if (permissions.isNotEmpty()) {
            sender.sendMessage(Component.text("Индивидуальные права: ", NamedTextColor.AQUA))
            permissions.forEach { perm ->
                sender.sendMessage(Component.text("  • ", NamedTextColor.GREEN)
                    .append(Component.text(perm, NamedTextColor.WHITE)))
            }
        } else {
            sender.sendMessage(Component.text("Индивидуальные права: ", NamedTextColor.AQUA)
                .append(Component.text("нет", NamedTextColor.GRAY)))
        }
        
        val allPermissions = PermissionDatabase.getEffectivePermissions(uuidObj.toString())
        sender.sendMessage(Component.text("Всего доступных прав: ", NamedTextColor.AQUA)
            .append(Component.text(allPermissions.size.toString(), NamedTextColor.YELLOW)))
    }
}
