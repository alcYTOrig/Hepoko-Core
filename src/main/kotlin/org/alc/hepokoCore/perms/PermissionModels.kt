package org.alc.hepokoCore.perms

import java.util.UUID

data class PermissionNode(
    val node: String,
    val value: Boolean = true,
    val context: String = "global"
)

data class GroupData(
    val name: String,
    val weight: Int,
    val permissions: MutableList<PermissionNode> = mutableListOf()
)

data class UserData(
    val uuid: UUID,
    val name: String,
    val groups: MutableList<String> = mutableListOf("default"),
    val permissions: MutableList<PermissionNode> = mutableListOf()
)
