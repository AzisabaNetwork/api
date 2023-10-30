package net.azisaba.api.server.interchat

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import net.azisaba.api.Logger
import net.azisaba.api.server.schemas.LuckPerms
import net.azisaba.api.server.util.Util
import net.azisaba.interchat.api.data.ChatMetaNodeData
import net.azisaba.interchat.api.data.UserDataProvider
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

object UserDataProviderImpl : UserDataProvider {
    private val requestDataCooldown = ConcurrentHashMap<UUID, Long>()
    private val prefix = ConcurrentHashMap<UUID, MutableMap<String, String>>()
    private val suffix = ConcurrentHashMap<UUID, MutableMap<String, String>>()
    private val client = HttpClient(CIO) {
        engine {
            requestTimeout = 1000 * 30
        }
    }

    override fun getPrefix(uuid: UUID): MutableMap<String, String> = prefix.computeIfAbsent(uuid) { mutableMapOf() }

    override fun getSuffix(uuid: UUID): MutableMap<String, String> = suffix.computeIfAbsent(uuid) { mutableMapOf() }

    fun requestDataAsync(uuid: UUID, server: String) {
        if ((requestDataCooldown[uuid] ?: 0) > System.currentTimeMillis()) return
        requestDataCooldown[uuid] = System.currentTimeMillis() + 10000
        val prefixes = prefix.computeIfAbsent(uuid) { mutableMapOf() }
        val suffixes = suffix.computeIfAbsent(uuid) { mutableMapOf() }
        InterChatApi.asyncExecutor.execute {
            runBlocking {
                val data = try {
                    client.get("https://interchat-userdata-worker.azisaba.workers.dev/userdata?uuid=$uuid&server=$server").bodyAsText()
                } catch (e: Exception) {
                    Logger.currentLogger.warn("Error querying userdata", e)
                    ""
                }
                prefixes[server] = data
                suffixes[server] = ""
                prefixes += ChatMetaNodeData.toMap(getChatMetaNodeDataList(uuid, "prefix"))
                suffixes += ChatMetaNodeData.toMap(getChatMetaNodeDataList(uuid, "suffix"))
            }
        }
    }

    // cached for an hour
    val getChatMetaNodeDataList = Util.memoize2<UUID, String, List<ChatMetaNodeData>>(1000 * 60 * 60) { uuid, type ->
        val list = mutableListOf<ChatMetaNodeData>()
        val user = LuckPerms.UserPermissions.getPermissions(uuid)
        val groupServerMap = mutableListOf<Pair<String, Set<String>>>()
        val weightOverride = mutableMapOf<String, Int>()
        val allInheritedGroups = mutableSetOf<String>()
        user.forEach { node ->
            if (node.permission.startsWith("group.")) {
                val groupName = node.permission.substring("group.".length)
                allInheritedGroups += groupName
                val servers = if (node.server == "global") emptySet() else setOf(node.server)
                if (servers.isEmpty()) {
                    groupServerMap.add(groupName to emptySet())
                    return@forEach
                }
                try {
                    groupServerMap.add(groupName to servers)
                } catch (ignored: UnsupportedOperationException) {}
            } else if (node.permission.startsWith("$type.") && node.value) {
                val servers = if (node.server == "global") emptySet() else setOf(node.server)
                val priorityAndValue = node.permission.removePrefix("$type.")
                val priority = priorityAndValue.substring(0, priorityAndValue.indexOf('.')).toInt()
                val metaValue = priorityAndValue.substring(priorityAndValue.indexOf('.') + 1)
                list.add(ChatMetaNodeData(servers, priority, metaValue, null))
            }
        }
        // create copy of list to prevent concurrent modification
        mutableListOf(*groupServerMap.toTypedArray()).forEach { (groupName, servers) ->
            val nodes = LuckPerms.GroupPermissions.getPermissions(groupName)
            val groupWeight = LuckPerms.GroupPermissions.getGroupWeight(groupName).toInt()
            nodes.forEach { node ->
                if (node.permission.startsWith("group.")) {
                    val inheritedGroupName = node.permission.removePrefix("group.")
                    allInheritedGroups += inheritedGroupName
                    groupServerMap.add(inheritedGroupName to servers)
                    val currentWeight = weightOverride[inheritedGroupName] ?: 0
                    weightOverride[inheritedGroupName] = max(currentWeight, groupWeight)
                }
            }
        }
        allInheritedGroups.forEach { groupName ->
            val thisGroupWeight = LuckPerms.GroupPermissions.getGroupWeight(groupName).toInt()
            val groupWeight = min(weightOverride[groupName] ?: Int.MAX_VALUE, thisGroupWeight)
            LuckPerms.GroupPermissions.getPermissions(groupName).forEach { node ->
                if (node.permission.startsWith("$type.")) {
                    var servers: MutableSet<String>? = if (node.server == "global") mutableSetOf() else mutableSetOf(node.server)
                    val groupServer = mutableSetOf<String>()
                    val priorityAndValue = node.permission.removePrefix("$type.")
                    val priority = priorityAndValue.substring(0, priorityAndValue.indexOf('.')).toInt()
                    val metaValue = priorityAndValue.substring(priorityAndValue.indexOf('.') + 1)
                    groupServerMap
                        .filter { it.first == groupName }
                        .sortedBy { it.second.size }
                        .forEach { (_, servers) ->
                            if (servers.isEmpty()) {
                                // user inherits the group without server= context
                                groupServer.clear()
                            } else {
                                groupServer += servers
                            }
                        }
                    if (groupServer.isNotEmpty()) {
                        if (servers?.isNotEmpty() == true) {
                            servers.retainAll(groupServer)
                            if (servers.isEmpty()) {
                                servers = null
                            }
                        } else {
                            servers?.addAll(groupServer)
                        }
                    }
                    list.add(ChatMetaNodeData(servers, priority, metaValue, groupWeight))
                }
            }
        }
        list
    }
}
