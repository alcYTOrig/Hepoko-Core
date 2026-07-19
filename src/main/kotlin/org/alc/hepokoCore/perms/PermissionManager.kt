package org.alc.hepokoCore.perms

import org.bukkit.entity.Player
import java.util.UUID

object PermissionManager {
    private val groupsCache = mutableMapOf<String, GroupData>()
    private val usersCache = mutableMapOf<UUID, UserData>()

    fun loadCache() {
        groupsCache.clear()
        val dbGroups = PermissionDatabase.getAllGroups()
        dbGroups.forEach { groupsCache[it.name.lowercase()] = it }
        println("[HepokoPerms] Загружено ${groupsCache.size} групп в кэш.")
    }

    fun getGroup(name: String): GroupData? = groupsCache[name.lowercase()]

    fun getAllGroups(): List<GroupData> = groupsCache.values.toList()

    fun getUserData(uuid: UUID): UserData? = usersCache[uuid]

    // Главная функция проверки прав
    fun hasPermission(player: Player, node: String): Boolean {
        val uuid = player.uniqueId

        // 1. Проверяем индивидуальные права игрока (приоритет выше)
        val user = usersCache[uuid]
        if (user != null) {
            val userPerm = user.permissions.find { it.node.equals(node, ignoreCase = true) }
            if (userPerm != null) return userPerm.value
        }

        // 2. Проверяем права групп игрока (сортируем по весу: от высшего к низшему)
        val playerGroups = user?.groups ?: listOf("default")
        val sortedGroups = playerGroups.mapNotNull { groupsCache[it.lowercase()] }
            .sortedByDescending { it.weight }

        for (group in sortedGroups) {
            val groupPerm = group.permissions.find { it.node.equals(node, ignoreCase = true) }
            if (groupPerm != null) return groupPerm.value
        }

        // 3. Если ничего не найдено, проверяем стандартный OP статус Minecraft
        return player.isOp
    }

    // Загрузка всех пользователей из БД
    fun loadAllUsers() {
        usersCache.clear()
        val dbUsers = PermissionDatabase.getAllUsers()
        dbUsers.forEach { usersCache[it.uuid] = it }
        println("[HepokoPerms] Загружено ${usersCache.size} пользователей в кэш.")
    }

    // Загрузка данных игрока при входе
    fun loadUser(player: Player) {
        if (!usersCache.containsKey(player.uniqueId)) {
            usersCache[player.uniqueId] = UserData(player.uniqueId, player.name)
        }
    }

    // Назначение пользователя в группу
    fun setUserGroup(uuid: java.util.UUID, groupName: String): Boolean {
        val result = PermissionDatabase.setUserPrimaryGroup(uuid.toString(), groupName)
        if (result) {
            // Обновляем кэш
            val userData = usersCache[uuid] ?: UserData(uuid, "unknown")
            userData.groups.clear()
            userData.groups.add(groupName.lowercase())
            usersCache[uuid] = userData
        }
        return result
    }

    // Получение групп пользователя
    fun getUserGroups(uuid: java.util.UUID): List<String> {
        return usersCache[uuid]?.groups ?: PermissionDatabase.getUserGroups(uuid.toString())
    }
}