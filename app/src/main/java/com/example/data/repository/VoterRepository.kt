package com.example.data.repository

import com.example.data.local.Voter
import com.example.data.local.VoterDao
import com.example.data.preferences.AppConfig
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.ThemeMode
import com.example.util.FuzzySearchHelper
import com.example.util.SearchInputMode
import com.example.util.TextEncodingSanitizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class VoterRepository(
    private val voterDao: VoterDao,
    private val appPreferences: AppPreferences
) {
    val appConfig: Flow<AppConfig> = appPreferences.configState

    fun searchAndFilterVoters(
        query: String,
        searchMode: SearchInputMode = SearchInputMode.CEDULA,
        votingPlaces: Set<String> = emptySet(),
        tableNumbers: Set<String> = emptySet(),
        citiesOrZones: Set<String> = emptySet(),
        barrios: Set<String> = emptySet(),
        votedFilter: Boolean? = null,
        hasPhoneFilter: Boolean? = null,
        notesKeyword: String = "",
        selectedGroupVoterIds: Set<Long>? = null,
        orderNumberFrom: String = "",
        orderNumberTo: String = ""
    ): Flow<List<Voter>> {
        val cleanQuery = query.trim()
        val cleanPlaces = votingPlaces.filter { it.isNotBlank() }.toSet()
        val cleanTables = tableNumbers.filter { it.isNotBlank() }.toSet()
        val cleanCities = citiesOrZones.filter { it.isNotBlank() }.toSet()
        val cleanBarrios = barrios.filter { it.isNotBlank() }.map { it.trim().lowercase() }.toSet()
        val cleanNotes = notesKeyword.trim()
        val minOrder = orderNumberFrom.trim().toIntOrNull()
        val maxOrder = orderNumberTo.trim().toIntOrNull()

        return voterDao.getAllVotersFlow().map { list ->
            var filtered = list

            // 1. Multi-filter Voting Places (Union of selected places)
            if (cleanPlaces.isNotEmpty()) {
                filtered = filtered.filter { voter -> cleanPlaces.contains(voter.votingPlace.trim()) }
            }

            // 2. Multi-filter Tables (Union of selected tables)
            if (cleanTables.isNotEmpty()) {
                filtered = filtered.filter { voter -> cleanTables.contains(voter.tableNumber.trim()) }
            }

            // 3. Multi-filter Cities / Zones (Union of selected cities/zones)
            if (cleanCities.isNotEmpty()) {
                filtered = filtered.filter { voter -> cleanCities.contains(voter.cityOrZone.trim()) }
            }

            // 4. Multi-filter Barrios / Addresses (Union of selected barrios)
            if (cleanBarrios.isNotEmpty()) {
                filtered = filtered.filter { voter ->
                    val addrLower = voter.address.lowercase()
                    val cityLower = voter.cityOrZone.lowercase()
                    cleanBarrios.any { b -> addrLower.contains(b) || cityLower.contains(b) }
                }
            }

            // 5. Filter by Voted Status
            if (votedFilter != null) {
                filtered = filtered.filter { it.voted == votedFilter }
            }

            // 6. Filter by Phone Availability
            if (hasPhoneFilter != null) {
                filtered = filtered.filter {
                    if (hasPhoneFilter) it.phone.isNotBlank() else it.phone.isBlank()
                }
            }

            // 7. Filter by Notes keyword
            if (cleanNotes.isNotEmpty()) {
                filtered = filtered.filter { it.notes.contains(cleanNotes, ignoreCase = true) }
            }

            // 8. Filter by Groups (Union of voter IDs in selected groups)
            if (selectedGroupVoterIds != null) {
                filtered = filtered.filter { selectedGroupVoterIds.contains(it.id) }
            }

            // 9. Filter by Order Number Range
            if (minOrder != null || maxOrder != null) {
                filtered = filtered.filter { voter ->
                    val voterOrder = voter.orderNumber.trim().toIntOrNull()
                    if (voterOrder != null) {
                        (minOrder == null || voterOrder >= minOrder) &&
                        (maxOrder == null || voterOrder <= maxOrder)
                    } else {
                        true
                    }
                }
            }

            // 10. Apply Search Query based on Mode (Cedula vs Fuzzy Name)
            if (cleanQuery.isNotEmpty()) {
                filtered = when (searchMode) {
                    SearchInputMode.CEDULA -> {
                        FuzzySearchHelper.filterByCedula(filtered, cleanQuery)
                    }
                    SearchInputMode.NOMBRE_APELLIDO -> {
                        FuzzySearchHelper.filterAndRankByName(filtered, cleanQuery)
                    }
                }
            }

            filtered
        }
    }

    val totalVoterCount: Flow<Int> = voterDao.getVoterCountFlow()
    val totalVotedCount: Flow<Int> = voterDao.getVotedCountFlow()
    val totalVotersWithPhoneCount: Flow<Int> = voterDao.getVotersWithPhoneCountFlow()
    val votingPlaces: Flow<List<String>> = voterDao.getVotingPlacesFlow()
    val tableNumbers: Flow<List<String>> = voterDao.getTableNumbersFlow()
    val citiesOrZones: Flow<List<String>> = voterDao.getCitiesOrZonesFlow()
    val addresses: Flow<List<String>> = voterDao.getAddressesFlow()
    val customBarrios: StateFlow<List<String>> = appPreferences.customBarriosState

    val allBarriosAndAddresses: Flow<List<String>> = kotlinx.coroutines.flow.combine(
        voterDao.getAddressesFlow(),
        voterDao.getCitiesOrZonesFlow(),
        appPreferences.customBarriosState
    ) { addressesList, citiesList, customList ->
        (customList + citiesList + addressesList)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun addCustomBarrio(barrio: String) = appPreferences.addCustomBarrio(barrio)
    fun removeCustomBarrio(barrio: String) = appPreferences.removeCustomBarrio(barrio)

    val isLoggedIn: kotlinx.coroutines.flow.StateFlow<Boolean> = appPreferences.isLoggedInState
    val voterGroups: kotlinx.coroutines.flow.StateFlow<List<com.example.data.model.VoterGroup>> = appPreferences.voterGroupsState

    fun verifyLogin(username: String, password: String): Boolean {
        val isValid = appPreferences.verifyCredentials(username, password)
        if (isValid) {
            appPreferences.setLoggedIn(true)
        }
        return isValid
    }

    fun logout() {
        appPreferences.setLoggedIn(false)
    }

    fun changePasswordWithAdminCode(adminCode: String, newPassword: String): Boolean {
        return appPreferences.changePasswordWithAdminCode(adminCode, newPassword)
    }

    fun createVoterGroup(name: String, voterIds: List<Long>): com.example.data.model.VoterGroup {
        return appPreferences.saveVoterGroup(name, voterIds)
    }

    fun updateVoterGroupName(groupId: String, newName: String) {
        appPreferences.updateVoterGroup(groupId, newName)
    }

    fun updateVoterGroup(groupId: String, newName: String, voterIds: List<Long>) {
        appPreferences.updateVoterGroup(groupId, newName, voterIds)
    }

    fun addVotersToGroup(groupId: String, voterIds: List<Long>) {
        appPreferences.addVotersToGroup(groupId, voterIds)
    }

    fun removeVoterFromGroup(groupId: String, voterId: Long) {
        appPreferences.removeVoterFromGroup(groupId, voterId)
    }

    fun deleteVoterGroup(groupId: String) {
        appPreferences.deleteVoterGroup(groupId)
    }

    suspend fun getVotersByIds(ids: List<Long>): List<Voter> {
        if (ids.isEmpty()) return emptyList()
        return voterDao.getVotersByIds(ids)
    }

    suspend fun markVotersStatusBulk(ids: List<Long>, voted: Boolean) {
        if (ids.isEmpty()) return
        voterDao.updateVotedStatusBulk(ids, voted)
    }

    suspend fun getAllVotersList(): List<Voter> = voterDao.getAllVotersList()
    suspend fun getVotersWithPhoneList(): List<Voter> = voterDao.getVotersWithPhoneList()

    suspend fun insertVoter(voter: Voter): Long = voterDao.insertVoter(TextEncodingSanitizer.sanitizeVoter(voter))

    suspend fun insertVoters(voters: List<Voter>) = voterDao.insertVoters(TextEncodingSanitizer.sanitizeVoters(voters))

    /**
     * Updates or imports voters while strictly preserving user modifications
     * (voted status, custom phone numbers, notes, and manually added voters).
     */
    suspend fun insertOrUpdateVotersPreservingData(incomingVotersRaw: List<Voter>) {
        if (incomingVotersRaw.isEmpty()) return
        val incomingVoters = TextEncodingSanitizer.sanitizeVoters(incomingVotersRaw)
        val existingVoters = voterDao.getAllVotersList()
        val existingByCedula = existingVoters.associateBy {
            val clean = it.cedula.replace(Regex("[^0-9]"), "")
            if (clean.isNotBlank()) clean else it.cedula.trim().lowercase()
        }

        val mergedList = mutableListOf<Voter>()
        val matchedExistingIds = mutableSetOf<Long>()

        for (incoming in incomingVoters) {
            val key = incoming.cedula.replace(Regex("[^0-9]"), "")
                .ifBlank { incoming.cedula.trim().lowercase() }
            val existing = existingByCedula[key]

            if (existing != null) {
                matchedExistingIds.add(existing.id)
                val merged = incoming.copy(
                    id = existing.id,
                    voted = existing.voted || incoming.voted,
                    phone = if (existing.phone.isNotBlank()) existing.phone else incoming.phone,
                    notes = if (existing.notes.isNotBlank()) existing.notes else incoming.notes,
                    updatedAt = System.currentTimeMillis()
                )
                mergedList.add(merged)
            } else {
                mergedList.add(incoming)
            }
        }

        // Preserve any existing voters that were not in the incoming file
        val unmergedExisting = existingVoters.filter { !matchedExistingIds.contains(it.id) }
        mergedList.addAll(unmergedExisting)

        voterDao.insertVoters(mergedList)
    }

    /**
     * Automatically repairs any existing records in the database that may contain corrupted
     * encoding or mojibake (e.g. Ã‘, Ã±, &#209;, \uFFFD in surnames like ACUÑA).
     */
    suspend fun sanitizeExistingDatabaseRecords() {
        try {
            val existing = voterDao.getAllVotersList()
            val needsUpdate = mutableListOf<Voter>()
            for (voter in existing) {
                val sanitized = TextEncodingSanitizer.sanitizeVoter(voter)
                if (sanitized != voter) {
                    needsUpdate.add(sanitized)
                }
            }
            if (needsUpdate.isNotEmpty()) {
                voterDao.insertVoters(needsUpdate)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateVoter(voter: Voter) = voterDao.updateVoter(TextEncodingSanitizer.sanitizeVoter(voter))

    suspend fun deleteVoter(voter: Voter) = voterDao.deleteVoter(voter)

    suspend fun clearAllVoters() = voterDao.clearAll()

    fun updateAppTitle(title: String) = appPreferences.updateTitle(title)

    fun updateFlyerUri(uri: String?) = appPreferences.updateFlyerUri(uri)

    fun updatePrimaryColor(colorHex: String) = appPreferences.updatePrimaryColor(colorHex)

    fun updateThemeMode(mode: ThemeMode) = appPreferences.updateThemeMode(mode)

    fun resetPreferences() = appPreferences.resetToDefaults()
}
