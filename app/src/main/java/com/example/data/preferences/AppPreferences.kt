package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.VoterGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class AppConfig(
    val appTitle: String = "Buscador de Votantes 2026",
    val flyerUri: String? = null,
    val primaryColorHex: String = "#1E3A8A", // Default Navy Blue
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("voter_app_settings", Context.MODE_PRIVATE)

    private val _configState = MutableStateFlow(loadConfig())
    val configState: StateFlow<AppConfig> = _configState.asStateFlow()

    private val _isLoggedInState = MutableStateFlow(loadLoginState())
    val isLoggedInState: StateFlow<Boolean> = _isLoggedInState.asStateFlow()

    private val _voterGroupsState = MutableStateFlow(loadVoterGroups())
    val voterGroupsState: StateFlow<List<VoterGroup>> = _voterGroupsState.asStateFlow()

    private val _customBarriosState = MutableStateFlow(loadCustomBarrios())
    val customBarriosState: StateFlow<List<String>> = _customBarriosState.asStateFlow()

    private fun loadLoginState(): Boolean {
        return prefs.getBoolean("key_is_logged_in", false)
    }

    private fun loadCustomBarrios(): List<String> {
        val jsonStr = prefs.getString("key_custom_barrios", "[]") ?: "[]"
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val b = array.getString(i).trim()
                if (b.isNotEmpty()) list.add(b)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun addCustomBarrio(barrio: String) {
        val clean = barrio.trim()
        if (clean.isEmpty()) return
        val current = loadCustomBarrios().toMutableList()
        if (!current.any { it.equals(clean, ignoreCase = true) }) {
            current.add(0, clean)
            saveCustomBarriosToDisk(current)
        }
    }

    fun removeCustomBarrio(barrio: String) {
        val current = loadCustomBarrios().filter { !it.equals(barrio.trim(), ignoreCase = true) }
        saveCustomBarriosToDisk(current)
    }

    private fun saveCustomBarriosToDisk(list: List<String>) {
        val array = JSONArray()
        list.forEach { array.put(it) }
        prefs.edit().putString("key_custom_barrios", array.toString()).apply()
        _customBarriosState.value = list
    }

    private fun loadVoterGroups(): List<VoterGroup> {
        val jsonStr = prefs.getString("key_voter_groups", "[]") ?: "[]"
        val list = mutableListOf<VoterGroup>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                val idsArray = obj.optJSONArray("voterIds") ?: JSONArray()
                val ids = mutableListOf<Long>()
                for (j in 0 until idsArray.length()) {
                    ids.add(idsArray.getLong(j))
                }
                list.add(VoterGroup(id, name, createdAt, ids))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveVoterGroup(name: String, voterIds: List<Long>): VoterGroup {
        val current = loadVoterGroups().toMutableList()
        val newGroup = VoterGroup(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "Grupo de Votantes" },
            createdAt = System.currentTimeMillis(),
            voterIds = voterIds.distinct()
        )
        current.add(0, newGroup)
        saveGroupsToDisk(current)
        return newGroup
    }

    fun updateVoterGroup(groupId: String, newName: String, voterIds: List<Long>? = null) {
        val current = loadVoterGroups().map { group ->
            if (group.id == groupId) {
                group.copy(
                    name = newName.trim().ifBlank { group.name },
                    voterIds = voterIds?.distinct() ?: group.voterIds
                )
            } else {
                group
            }
        }
        saveGroupsToDisk(current)
    }

    fun addVotersToGroup(groupId: String, newVoterIds: List<Long>) {
        if (newVoterIds.isEmpty()) return
        val current = loadVoterGroups().map { group ->
            if (group.id == groupId) {
                val combined = (group.voterIds + newVoterIds).distinct()
                group.copy(voterIds = combined)
            } else {
                group
            }
        }
        saveGroupsToDisk(current)
    }

    fun removeVoterFromGroup(groupId: String, voterId: Long) {
        val current = loadVoterGroups().map { group ->
            if (group.id == groupId) {
                group.copy(voterIds = group.voterIds.filter { it != voterId })
            } else {
                group
            }
        }
        saveGroupsToDisk(current)
    }

    fun deleteVoterGroup(groupId: String) {
        val current = loadVoterGroups().filter { it.id != groupId }
        saveGroupsToDisk(current)
    }

    private fun saveGroupsToDisk(groups: List<VoterGroup>) {
        val array = JSONArray()
        for (g in groups) {
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("name", g.name)
            obj.put("createdAt", g.createdAt)
            val vArr = JSONArray()
            g.voterIds.forEach { vArr.put(it) }
            obj.put("voterIds", vArr)
            array.put(obj)
        }
        prefs.edit().putString("key_voter_groups", array.toString()).apply()
        _voterGroupsState.value = groups
    }

    private fun loadConfig(): AppConfig {
        return AppConfig(
            appTitle = prefs.getString("key_app_title", "Buscador de Votantes 2026") ?: "Buscador de Votantes 2026",
            flyerUri = prefs.getString("key_flyer_uri", null),
            primaryColorHex = prefs.getString("key_primary_color", "#1E3A8A") ?: "#1E3A8A",
            themeMode = try {
                ThemeMode.valueOf(prefs.getString("key_theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            }
        )
    }

    fun getStoredUsername(): String {
        return prefs.getString("key_auth_username", "lista1") ?: "lista1"
    }

    fun getStoredPassword(): String {
        return prefs.getString("key_auth_password", "paraguay2026") ?: "paraguay2026"
    }

    fun verifyCredentials(usernameInput: String, passwordInput: String): Boolean {
        val currentUsername = getStoredUsername()
        val currentPassword = getStoredPassword()
        return usernameInput.trim().equals(currentUsername.trim(), ignoreCase = true) &&
                passwordInput == currentPassword
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        prefs.edit().putBoolean("key_is_logged_in", isLoggedIn).apply()
        _isLoggedInState.value = isLoggedIn
    }

    fun changePasswordWithAdminCode(adminCode: String, newPassword: String): Boolean {
        if (adminCode.trim() == "admin2026") {
            prefs.edit().putString("key_auth_password", newPassword).apply()
            return true
        }
        return false
    }

    fun updateTitle(newTitle: String) {
        prefs.edit().putString("key_app_title", newTitle.ifBlank { "Buscador de Votantes 2026" }).apply()
        _configState.value = loadConfig()
    }

    fun updateFlyerUri(uriString: String?) {
        prefs.edit().putString("key_flyer_uri", uriString).apply()
        _configState.value = loadConfig()
    }

    fun updatePrimaryColor(colorHex: String) {
        prefs.edit().putString("key_primary_color", colorHex).apply()
        _configState.value = loadConfig()
    }

    fun updateThemeMode(mode: ThemeMode) {
        prefs.edit().putString("key_theme_mode", mode.name).apply()
        _configState.value = loadConfig()
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _configState.value = loadConfig()
    }
}
