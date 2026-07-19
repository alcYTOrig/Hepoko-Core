package org.alc.hepokoCore.perms

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class HepokoPermsCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(
                Component.text()
                    .append(Component.text("=== HepokoPerms ===\n", NamedTextColor.GOLD))
                    .append(Component.text("Управление группами:\n", NamedTextColor.AQUA))
                    .append(Component.text("/hperm group create <группа> <вес>\n", NamedTextColor.GREEN))
                    .append(Component.text("/hperm group <группа> add <нода>\n", NamedTextColor.GREEN))
                    .append(Component.text("/hperm group <группа> remove <нода>\n", NamedTextColor.RED))
                    .append(Component.text("/hperm group <группа> delete\n", NamedTextColor.DARK_RED))
                    .append(Component.text("\nУправление пользователями:\n", NamedTextColor.AQUA))
                    .append(Component.text("/hperm user <игрок> add <нода>\n", NamedTextColor.GREEN))
                    .append(Component.text("/hperm user <игрок> remove <нода>\n", NamedTextColor.RED))
                    .append(Component.text("/hperm user <игрок> group set <группа>\n", NamedTextColor.YELLOW))
                    .append(Component.text("/hperm user <игрок> group list\n", NamedTextColor.WHITE))
                    .append(Component.text("\nУправление правами групп:\n", NamedTextColor.AQUA))
                    .append(Component.text("/hperm list groups\n", NamedTextColor.WHITE))
                    .append(Component.text("/hperm list users\n", NamedTextColor.WHITE))
                    .append(Component.text("\nПерезагрузка:\n", NamedTextColor.AQUA))
                    .append(Component.text("/hperm reload", NamedTextColor.GOLD))
                    .build()
            )
            return true
        }

        val targetType = args[0].lowercase()

        when (targetType) {
            "group" -> {
                if (args.size < 2) {
                    sender.sendMessage(Component.text("Использование: /hperm group <действие> [аргументы]", NamedTextColor.RED))
                    return true
                }
                
                // Специальная обработка для команды create
                if (args[1].equals("create", ignoreCase = true) && args.size >= 4) {
                    val groupName = args[2]
                    val weight = args[3].toIntOrNull()
                    if (weight == null || weight < 0) {
                        sender.sendMessage(Component.text("Укажите корректный вес группы (число >= 0)!", NamedTextColor.RED))
                        return true
                    }
                    handleGroupCommand(sender, groupName, "create", "", weight)
                } else if (args.size >= 3) {
                    val groupName = args[1]
                    val action = args[2].lowercase()
                    val node = if (args.size > 3) args[3] else ""
                    val weight = if (args.size > 3) args[3].toIntOrNull() else null
                    handleGroupCommand(sender, groupName, action, node, weight)
                } else {
                    sender.sendMessage(Component.text("Использование: /hperm group <группа> <действие> [аргументы]", NamedTextColor.RED))
                    return true
                }
            }
            "user" -> {
                if (args.size < 3) {
                    sender.sendMessage(Component.text("Использование: /hperm user <игрок> <действие> [аргументы]", NamedTextColor.RED))
                    return true
                }
                val userName = args[1]
                val action = args[2].lowercase()
                val nodeOrGroup = if (args.size > 3) args[3] else ""
                val groupName = if (args.size > 4) args[4] else ""
                handleUserCommand(sender, userName, action, nodeOrGroup, groupName)
            }
            "list" -> {
                if (args.size < 2) {
                    sender.sendMessage(Component.text("Использование: /hperm list <groups|users>", NamedTextColor.RED))
                    return true
                }
                handleListCommand(sender, args[1].lowercase())
            }
            "reload" -> {
                PermissionManager.loadCache()
                PermissionManager.loadAllUsers()
                sender.sendMessage(Component.text("✔ Конфигурация HepokoPerms перезагружена!", NamedTextColor.GREEN))
                return true
            }
            else -> {
                sender.sendMessage(
                    Component.text()
                        .append(Component.text("Неизвестная команда. Используйте ", NamedTextColor.RED))
                        .append(Component.text("/hperm", NamedTextColor.YELLOW))
                        .append(Component.text(" для списка команд.", NamedTextColor.RED))
                        .build()
                )
            }
        }
        return true
    }

    private fun handleGroupCommand(sender: CommandSender, groupName: String, action: String, node: String, weight: Int?) {
        val groupNameLower = groupName.lowercase()

        when (action) {
            "add" -> {
                if (node.isEmpty()) {
                    sender.sendMessage(Component.text("Укажите ноду права!", NamedTextColor.RED))
                    return
                }
                PermissionDatabase.addGroupPermission(groupNameLower, node, true)
                PermissionManager.loadCache()

                sender.sendMessage(
                    Component.text()
                        .append(Component.text("✔ Право '", NamedTextColor.GREEN))
                        .append(Component.text(node, NamedTextColor.YELLOW))
                        .append(Component.text("' добавлено группе '", NamedTextColor.GREEN))
                        .append(Component.text(groupName, NamedTextColor.AQUA))
                        .append(Component.text("'", NamedTextColor.GREEN))
                        .build()
                )
            }
            "remove" -> {
                if (node.isEmpty()) {
                    sender.sendMessage(Component.text("Укажите ноду права для удаления!", NamedTextColor.RED))
                    return
                }
                val removed = PermissionDatabase.removeGroupPermission(groupNameLower, node)
                if (removed) {
                    PermissionManager.loadCache()
                    sender.sendMessage(
                        Component.text()
                            .append(Component.text("✔ Право '", NamedTextColor.GREEN))
                            .append(Component.text(node, NamedTextColor.YELLOW))
                            .append(Component.text("' удалено у группы '", NamedTextColor.GREEN))
                            .append(Component.text(groupName, NamedTextColor.AQUA))
                            .append(Component.text("'", NamedTextColor.GREEN))
                            .build()
                    )
                } else {
                    sender.sendMessage(Component.text("Право не найдено у этой группы", NamedTextColor.RED))
                }
            }
            "delete" -> {
                if (groupNameLower == "default") {
                    sender.sendMessage(Component.text("Нельзя удалить группу 'default'!", NamedTextColor.RED))
                    return
                }
                val removed = PermissionDatabase.removeGroup(groupNameLower)
                if (removed) {
                    PermissionManager.loadCache()
                    sender.sendMessage(
                        Component.text()
                            .append(Component.text("✔ Группа '", NamedTextColor.GREEN))
                            .append(Component.text(groupName, NamedTextColor.AQUA))
                            .append(Component.text("' и все её права удалены", NamedTextColor.GREEN))
                            .build()
                    )
                } else {
                    sender.sendMessage(Component.text("Группа не найдена", NamedTextColor.RED))
                }
            }
            "create" -> {
                if (weight == null || weight < 0) {
                    sender.sendMessage(Component.text("Укажите вес группы (число >= 0)!", NamedTextColor.RED))
                    return
                }
                PermissionDatabase.addGroup(groupNameLower, weight)
                PermissionManager.loadCache()
                sender.sendMessage(
                    Component.text()
                        .append(Component.text("✔ Группа '", NamedTextColor.GREEN))
                        .append(Component.text(groupName, NamedTextColor.AQUA))
                        .append(Component.text("' создана с весом ", NamedTextColor.GREEN))
                        .append(Component.text(weight.toString(), NamedTextColor.YELLOW))
                        .build()
                )
            }
            else -> {
                sender.sendMessage(Component.text("Неизвестное действие. Используйте 'add', 'remove', 'delete' или 'create'", NamedTextColor.RED))
            }
        }
    }

    private fun handleUserCommand(sender: CommandSender, userName: String, action: String, nodeOrGroup: String, groupName: String) {
        when (action) {
            "add" -> {
                if (nodeOrGroup.isEmpty()) {
                    sender.sendMessage(Component.text("Укажите ноду права!", NamedTextColor.RED))
                    return
                }
                val player = Bukkit.getPlayer(userName)
                val uuid = player?.uniqueId?.toString()

                if (uuid == null) {
                    sender.sendMessage(
                        Component.text()
                            .append(Component.text("⚠ Игрок ", NamedTextColor.YELLOW))
                            .append(Component.text(userName, NamedTextColor.WHITE))
                            .append(Component.text(" оффлайн. Индивидуальные права работают только для онлайн-игроков.", NamedTextColor.YELLOW))
                            .build()
                    )
                    return
                }

                PermissionDatabase.addUserPermission(uuid, nodeOrGroup, true)

                sender.sendMessage(
                    Component.text()
                        .append(Component.text("✔ Право '", NamedTextColor.GREEN))
                        .append(Component.text(nodeOrGroup, NamedTextColor.YELLOW))
                        .append(Component.text("' выдано игроку '", NamedTextColor.GREEN))
                        .append(Component.text(userName, NamedTextColor.AQUA))
                        .append(Component.text("'", NamedTextColor.GREEN))
                        .build()
                )

                player.sendMessage(
                    Component.text()
                        .append(Component.text("Вам выдано право: ", NamedTextColor.GREEN))
                        .append(Component.text(nodeOrGroup, NamedTextColor.YELLOW))
                        .build()
                )
            }
            "remove" -> {
                if (nodeOrGroup.isEmpty()) {
                    sender.sendMessage(Component.text("Укажите ноду права для удаления!", NamedTextColor.RED))
                    return
                }
                val player = Bukkit.getPlayer(userName)
                val uuid = player?.uniqueId?.toString()

                if (uuid == null) {
                    sender.sendMessage(
                        Component.text()
                            .append(Component.text("⚠ Игрок ", NamedTextColor.YELLOW))
                            .append(Component.text(userName, NamedTextColor.WHITE))
                            .append(Component.text(" оффлайн.", NamedTextColor.YELLOW))
                            .build()
                    )
                    return
                }

                val removed = PermissionDatabase.removeUserPermission(uuid, nodeOrGroup)
                if (removed) {
                    sender.sendMessage(
                        Component.text()
                            .append(Component.text("✔ Право '", NamedTextColor.GREEN))
                            .append(Component.text(nodeOrGroup, NamedTextColor.YELLOW))
                            .append(Component.text("' удалено у игрока '", NamedTextColor.GREEN))
                            .append(Component.text(userName, NamedTextColor.AQUA))
                            .append(Component.text("'", NamedTextColor.GREEN))
                            .build()
                    )

                    player.sendMessage(
                        Component.text()
                            .append(Component.text("У вас удалено право: ", NamedTextColor.RED))
                            .append(Component.text(nodeOrGroup, NamedTextColor.YELLOW))
                            .build()
                    )
                } else {
                    sender.sendMessage(Component.text("Право не найдено у этого игрока", NamedTextColor.RED))
                }
            }
            "group" -> {
                when (nodeOrGroup.lowercase()) {
                    "set" -> {
                        if (groupName.isEmpty()) {
                            sender.sendMessage(Component.text("Укажите имя группы!", NamedTextColor.RED))
                            return
                        }
                        val player = Bukkit.getPlayer(userName)
                        val uuid = player?.uniqueId

                        if (uuid == null) {
                            sender.sendMessage(
                                Component.text()
                                    .append(Component.text("⚠ Игрок ", NamedTextColor.YELLOW))
                                    .append(Component.text(userName, NamedTextColor.WHITE))
                                    .append(Component.text(" оффлайн. Используйте онлайн игрока.", NamedTextColor.YELLOW))
                                    .build()
                            )
                            return
                        }

                        PermissionManager.setUserGroup(uuid, groupName)
                        sender.sendMessage(
                            Component.text()
                                .append(Component.text("✔ Игрок '", NamedTextColor.GREEN))
                                .append(Component.text(userName, NamedTextColor.AQUA))
                                .append(Component.text("' добавлен в группу '", NamedTextColor.GREEN))
                                .append(Component.text(groupName, NamedTextColor.YELLOW))
                                .append(Component.text("'", NamedTextColor.GREEN))
                                .build()
                        )
                        player.sendMessage(
                            Component.text()
                                .append(Component.text("Вы добавлены в группу: ", NamedTextColor.GREEN))
                                .append(Component.text(groupName, NamedTextColor.YELLOW))
                                .build()
                        )
                    }
                    "list" -> {
                        val player = Bukkit.getPlayer(userName)
                        val uuid = player?.uniqueId

                        if (uuid == null) {
                            sender.sendMessage(
                                Component.text()
                                    .append(Component.text("⚠ Игрок ", NamedTextColor.YELLOW))
                                    .append(Component.text(userName, NamedTextColor.WHITE))
                                    .append(Component.text(" оффлайн. Показываю сохранённые группы.", NamedTextColor.YELLOW))
                                    .build()
                            )
                        }

                        val groups = PermissionManager.getUserGroups(uuid ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000"))
                        sender.sendMessage(
                            Component.text()
                                .append(Component.text("Группы игрока ", NamedTextColor.AQUA))
                                .append(Component.text(userName, NamedTextColor.WHITE))
                                .append(Component.text(": ", NamedTextColor.AQUA))
                                .append(Component.text(groups.joinToString(", "), NamedTextColor.YELLOW))
                                .build()
                        )
                    }
                    else -> {
                        sender.sendMessage(Component.text("Неизвестное действие для группы. Используйте 'set' или 'list'", NamedTextColor.RED))
                    }
                }
            }
            else -> {
                sender.sendMessage(Component.text("Неизвестное действие. Используйте 'add', 'remove' или 'group'", NamedTextColor.RED))
            }
        }
    }

    private fun handleListCommand(sender: CommandSender, target: String) {
        when (target) {
            "groups" -> {
                val groups = PermissionManager.getAllGroups()
                if (groups.isEmpty()) {
                    sender.sendMessage(Component.text("Группы не найдены.", NamedTextColor.RED))
                    return
                }
                
                sender.sendMessage(Component.text("=== Список групп ===", NamedTextColor.GOLD))
                groups.forEach { group ->
                    sender.sendMessage(
                        Component.text()
                            .append(Component.text("  - ", NamedTextColor.GRAY))
                            .append(Component.text(group.name, NamedTextColor.AQUA))
                            .append(Component.text(" (вес: ", NamedTextColor.WHITE))
                            .append(Component.text(group.weight.toString(), NamedTextColor.YELLOW))
                            .append(Component.text(")", NamedTextColor.WHITE))
                            .build()
                    )
                }
            }
            "users" -> {
                val users = PermissionDatabase.getAllUsers()
                if (users.isEmpty()) {
                    sender.sendMessage(Component.text("Пользователи не найдены.", NamedTextColor.RED))
                    return
                }
                
                sender.sendMessage(Component.text("=== Список пользователей ===", NamedTextColor.GOLD))
                users.forEach { user ->
                    sender.sendMessage(
                        Component.text()
                            .append(Component.text("  - ", NamedTextColor.GRAY))
                            .append(Component.text(user.name, NamedTextColor.AQUA))
                            .append(Component.text(" (UUID: ", NamedTextColor.WHITE))
                            .append(Component.text(user.uuid.toString().take(8), NamedTextColor.YELLOW))
                            .append(Component.text("...)", NamedTextColor.WHITE))
                            .append(Component.text(" Группа: ", NamedTextColor.WHITE))
                            .append(Component.text(user.groups.joinToString(", "), NamedTextColor.GREEN))
                            .build()
                    )
                }
            }
            else -> {
                sender.sendMessage(Component.text("Неизвестная цель. Используйте 'groups' или 'users'", NamedTextColor.RED))
            }
        }
    }
}