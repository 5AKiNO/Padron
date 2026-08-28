package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.Voter
import com.example.data.preferences.AppConfig
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.ThemeMode
import com.example.data.repository.VoterRepository
import com.example.data.auth.AuthRepository
import com.example.data.auth.LoginResult
import com.example.data.auth.LogoutResult
import com.example.util.DeviceIdHelper
import com.example.util.NetworkMonitor
import com.example.util.ExcelFormat
import com.example.util.ExcelReportExporter
import com.example.util.PdfReportExporter
import com.example.util.SampleDataProvider
import com.example.util.SearchInputMode
import com.example.util.VCardExporter
import com.example.util.XlsCsvParser
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FilterParams(
    val query: String,
    val searchMode: SearchInputMode,
    val filter: FilterState,
    val groups: List<com.example.data.model.VoterGroup>
)

data class FilterState(
    val selectedVotingPlaces: Set<String> = emptySet(),
    val selectedTableNumbers: Set<String> = emptySet(),
    val selectedCitiesOrZones: Set<String> = emptySet(),
    val selectedBarrios: Set<String> = emptySet(),
    val selectedGroupIds: Set<String> = emptySet(),
    val votedFilter: Boolean? = null, // null: all, true: voted, false: pending
    val hasPhoneFilter: Boolean? = null, // null: all, true: con teléfono, false: sin teléfono
    val notesKeyword: String = "",
    val orderNumberFrom: String = "",
    val orderNumberTo: String = "",
    val customPreset: String? = null // e.g. "PENDING_WITH_PHONE", "CONFIRMED_VOTED", "HAS_NOTES"
) {
    val selectedVotingPlace: String get() = selectedVotingPlaces.firstOrNull() ?: ""
    val selectedTableNumber: String get() = selectedTableNumbers.firstOrNull() ?: ""
    val selectedCityOrZone: String get() = selectedCitiesOrZones.firstOrNull() ?: ""
    val selectedAddressOrBarrio: String get() = selectedBarrios.firstOrNull() ?: ""
    val selectedGroupId: String? get() = selectedGroupIds.firstOrNull()

    fun activeCount(): Int {
        var count = 0
        count += selectedVotingPlaces.size
        count += selectedTableNumbers.size
        count += selectedCitiesOrZones.size
        count += selectedBarrios.size
        count += selectedGroupIds.size
        if (votedFilter != null) count++
        if (hasPhoneFilter != null) count++
        if (notesKeyword.isNotBlank()) count++
        if (orderNumberFrom.isNotBlank() || orderNumberTo.isNotBlank()) count++
        if (!customPreset.isNullOrBlank()) count++
        return count
    }
}

data class ImportState(
    val isImporting: Boolean = false,
    val message: String? = null,
    val previewVoters: List<Voter> = emptyList(),
    val totalProcessedLines: Int = 0,
    val headersFound: List<String> = emptyList()
)

data class ExportResult(
    val fileUri: Uri? = null,
    val mimeType: String = "",
    val message: String = ""
)

class VoterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VoterRepository
    private val authRepository = AuthRepository(application)
    private val networkMonitor = NetworkMonitor(application)
    private var sessionListener: ValueEventListener? = null

    val appConfig: StateFlow<AppConfig>

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnlineFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        networkMonitor.isOnline()
    )

    private val _isLoggedIn = MutableStateFlow(authRepository.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isLogging = MutableStateFlow(false)
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()

    private val _loginResult = MutableStateFlow<LoginResult?>(null)
    val loginResult: StateFlow<LoginResult?> = _loginResult.asStateFlow()

    private val _logoutResult = MutableStateFlow<LogoutResult?>(null)
    val logoutResult: StateFlow<LogoutResult?> = _logoutResult.asStateFlow()

    private val _sessionBlockedMessage = MutableStateFlow<String?>(null)
    val sessionBlockedMessage: StateFlow<String?> = _sessionBlockedMessage.asStateFlow()

    private val _noInternetLogoutMessage = MutableStateFlow<String?>(null)
    val noInternetLogoutMessage: StateFlow<String?> = _noInternetLogoutMessage.asStateFlow()

    private val _sessionRevokedMessage = MutableStateFlow<String?>(null)
    val sessionRevokedMessage: StateFlow<String?> = _sessionRevokedMessage.asStateFlow()

    // Dynamic Session Timer for Limited Users
    private val _timeLeft = MutableStateFlow(authRepository.getTrialMinutes() * 60L)
    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()

    private val _isLimitedUser = MutableStateFlow(!authRepository.isUnlimited())
    val isLimitedUser: StateFlow<Boolean> = _isLimitedUser.asStateFlow()

    private val _showTimeoutNoInternetDialog = MutableStateFlow(false)
    val showTimeoutNoInternetDialog: StateFlow<Boolean> = _showTimeoutNoInternetDialog.asStateFlow()

    private var sessionTimerJob: kotlinx.coroutines.Job? = null

    val currentDeviceId: String = authRepository.getCurrentDeviceId()
    val savedUsername: String? get() = authRepository.getSavedUsername()
    val trialMinutes: Long get() = authRepository.getTrialMinutes()

    init {
        val database = AppDatabase.getInstance(application)
        val preferences = AppPreferences(application)
        repository = VoterRepository(database.voterDao(), preferences)
        appConfig = repository.appConfig.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            AppConfig()
        )

        // Automatically repair any legacy or imported records with corrupted characters/mojibake
        viewModelScope.launch(Dispatchers.IO) {
            repository.sanitizeExistingDatabaseRecords()
        }

        // Verify active session status on startup
        viewModelScope.launch(Dispatchers.IO) {
            if (authRepository.isLoggedIn()) {
                val isValid = authRepository.checkSessionStatus()
                _isLoggedIn.value = isValid
                if (isValid) {
                    val username = authRepository.getSavedUsername()
                    if (!username.isNullOrBlank()) {
                        attachSessionListener(username)
                    }
                    startSessionTimer()
                }
            }
        }
    }

    fun startSessionTimer() {
        sessionTimerJob?.cancel()
        val isLimited = !authRepository.isUnlimited()
        _isLimitedUser.value = isLimited

        if (!isLimited) return

        val loginTime = authRepository.getLoginTime().let { if (it <= 0L) System.currentTimeMillis() else it }
        val totalSeconds = authRepository.getTrialMinutes() * 60L
        _timeLeft.value = totalSeconds

        sessionTimerJob = viewModelScope.launch {
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val elapsedSeconds = (currentTime - loginTime) / 1000
                val remaining = (totalSeconds - elapsedSeconds).coerceAtLeast(0L)
                _timeLeft.value = remaining

                if (remaining <= 0L) {
                    _timeLeft.value = 0L
                    attemptAutoLogoutOnTimeout()
                    break
                }
                delay(1000)
            }
        }
    }

    private fun stopSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = null
    }

    private suspend fun attemptAutoLogoutOnTimeout() {
        val username = authRepository.getSavedUsername()
        val trialMins = authRepository.getTrialMinutes()
        val result = authRepository.logout(username)
        when (result) {
            is LogoutResult.Success -> {
                detachSessionListener()
                stopSessionTimer()
                _isLoggedIn.value = false
                _showTimeoutNoInternetDialog.value = false
                _loginError.value = "Tu tiempo de uso ($trialMins min) ha expirado. Sesión cerrada."
            }
            is LogoutResult.NoInternet, is LogoutResult.Error -> {
                _showTimeoutNoInternetDialog.value = true
            }
        }
    }

    fun retryTimeoutLogout() {
        viewModelScope.launch {
            attemptAutoLogoutOnTimeout()
        }
    }

    private fun attachSessionListener(username: String) {
        detachSessionListener(username)
        sessionListener = authRepository.listenToSession(username) {
            _isLoggedIn.value = false
            _sessionRevokedMessage.value = "Tu sesión ha sido cerrada o transferida a otro dispositivo."
        }
    }

    private fun detachSessionListener(username: String? = null) {
        val user = username ?: authRepository.getSavedUsername()
        if (user != null && sessionListener != null) {
            authRepository.removeSessionListener(user, sessionListener!!)
            sessionListener = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        detachSessionListener()
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchInputMode = MutableStateFlow(SearchInputMode.CEDULA)
    val searchInputMode: StateFlow<SearchInputMode> = _searchInputMode.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _importState = MutableStateFlow(ImportState())
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _selectedVoter = MutableStateFlow<Voter?>(null)
    val selectedVoter: StateFlow<Voter?> = _selectedVoter.asStateFlow()

    private val _voterToEdit = MutableStateFlow<Voter?>(null)
    val voterToEdit: StateFlow<Voter?> = _voterToEdit.asStateFlow()

    private val _isAddVoterDialogOpen = MutableStateFlow(false)
    val isAddVoterDialogOpen: StateFlow<Boolean> = _isAddVoterDialogOpen.asStateFlow()

    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult: StateFlow<ExportResult?> = _exportResult.asStateFlow()

    val voterGroups: StateFlow<List<com.example.data.model.VoterGroup>> = repository.voterGroups

    private val _selectedVoterIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedVoterIds: StateFlow<Set<Long>> = _selectedVoterIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    fun enterSelectionMode(initialVoterId: Long? = null) {
        _isSelectionMode.value = true
        if (initialVoterId != null) {
            _selectedVoterIds.value = setOf(initialVoterId)
        }
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedVoterIds.value = emptySet()
    }

    fun toggleVoterSelection(voterId: Long) {
        val current = _selectedVoterIds.value.toMutableSet()
        if (current.contains(voterId)) {
            current.remove(voterId)
        } else {
            current.add(voterId)
        }
        _selectedVoterIds.value = current
        if (current.isEmpty() && _isSelectionMode.value) {
            // Keep in selection mode or let user cancel
        }
    }

    fun selectAllVisibleVoters(list: List<Voter>) {
        _isSelectionMode.value = true
        _selectedVoterIds.value = list.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedVoterIds.value = emptySet()
    }

    fun createGroupFromSelection(name: String): com.example.data.model.VoterGroup? {
        val ids = _selectedVoterIds.value.toList()
        if (ids.isEmpty()) return null
        val group = repository.createVoterGroup(name, ids)
        return group
    }

    fun deleteGroup(groupId: String) {
        repository.deleteVoterGroup(groupId)
        if (_filterState.value.selectedGroupIds.contains(groupId)) {
            _filterState.value = _filterState.value.copy(
                selectedGroupIds = _filterState.value.selectedGroupIds - groupId
            )
        }
    }

    fun updateGroupName(groupId: String, newName: String) {
        repository.updateVoterGroupName(groupId, newName)
    }

    fun updateGroup(groupId: String, newName: String, voterIds: List<Long>) {
        repository.updateVoterGroup(groupId, newName, voterIds)
    }

    fun addVotersToGroup(groupId: String, voterIds: List<Long>) {
        repository.addVotersToGroup(groupId, voterIds)
    }

    fun removeVoterFromGroup(groupId: String, voterId: Long) {
        repository.removeVoterFromGroup(groupId, voterId)
    }

    fun markGroupVotersStatus(group: com.example.data.model.VoterGroup, voted: Boolean) {
        if (group.voterIds.isEmpty()) return
        viewModelScope.launch {
            repository.markVotersStatusBulk(group.voterIds, voted)
        }
    }

    fun bulkMarkSelectedAsVotante(voted: Boolean) {
        val ids = _selectedVoterIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.markVotersStatusBulk(ids, voted)
            exitSelectionMode()
        }
    }

    fun getVotersByIds(ids: List<Long>, onResult: (List<Voter>) -> Unit) {
        viewModelScope.launch {
            val list = repository.getVotersByIds(ids)
            onResult(list)
        }
    }

    fun exportGroupToPdf(group: com.example.data.model.VoterGroup, onResult: (Uri?, List<Voter>) -> Unit) {
        viewModelScope.launch {
            val members = repository.getVotersByIds(group.voterIds)
            if (members.isEmpty()) {
                onResult(null, emptyList())
                return@launch
            }
            val config = appConfig.value
            val uri = withContext(Dispatchers.IO) {
                PdfReportExporter.generateVotersPdf(
                    context = getApplication(),
                    reportTitle = config.appTitle,
                    subtitle = "Grupo: ${group.name}",
                    primaryColorHex = config.primaryColorHex,
                    voters = members
                )
            }
            onResult(uri, members)
        }
    }

    fun exportGroupToExcel(
        group: com.example.data.model.VoterGroup,
        format: ExcelFormat = ExcelFormat.XLSX,
        onResult: (Uri?, List<Voter>, String) -> Unit
    ) {
        viewModelScope.launch {
            val members = repository.getVotersByIds(group.voterIds)
            if (members.isEmpty()) {
                onResult(null, emptyList(), format.mimeType)
                return@launch
            }
            val config = appConfig.value
            val result = withContext(Dispatchers.IO) {
                ExcelReportExporter.generateVotersSpreadsheet(
                    context = getApplication(),
                    reportTitle = config.appTitle,
                    subtitle = "Grupo: ${group.name}",
                    voters = members,
                    format = format
                )
            }
            onResult(result?.first, members, format.mimeType)
        }
    }

    fun exportGroupToVcf(
        group: com.example.data.model.VoterGroup,
        onResult: (Uri?, Int) -> Unit
    ) {
        viewModelScope.launch {
            val members = repository.getVotersByIds(group.voterIds)
            val membersWithPhone = members.filter { it.phone.trim().isNotBlank() }
            if (membersWithPhone.isEmpty()) {
                onResult(null, 0)
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                VCardExporter.generateVotersVcf(
                    context = getApplication(),
                    voters = membersWithPhone,
                    categoryOrGroup = group.name,
                    fileNamePrefix = "Grupo_${group.name.replace(Regex("[^a-zA-Z0-9]"), "_")}"
                )
            }
            onResult(result?.first, result?.second ?: 0)
        }
    }

    fun exportSelectedVotersToPdf(onResult: (Uri?, Int) -> Unit) {
        val ids = _selectedVoterIds.value.toList()
        if (ids.isEmpty()) {
            onResult(null, 0)
            return
        }
        viewModelScope.launch {
            val voters = repository.getVotersByIds(ids)
            val config = appConfig.value
            val uri = withContext(Dispatchers.IO) {
                PdfReportExporter.generateVotersPdf(
                    context = getApplication(),
                    reportTitle = config.appTitle,
                    subtitle = "Votantes Seleccionados (${voters.size})",
                    primaryColorHex = config.primaryColorHex,
                    voters = voters
                )
            }
            onResult(uri, voters.size)
        }
    }

    fun exportSelectedVotersToExcel(
        format: ExcelFormat = ExcelFormat.XLSX,
        onResult: (Uri?, Int, String) -> Unit
    ) {
        val ids = _selectedVoterIds.value.toList()
        if (ids.isEmpty()) {
            onResult(null, 0, format.mimeType)
            return
        }
        viewModelScope.launch {
            val voters = repository.getVotersByIds(ids)
            val config = appConfig.value
            val result = withContext(Dispatchers.IO) {
                ExcelReportExporter.generateVotersSpreadsheet(
                    context = getApplication(),
                    reportTitle = config.appTitle,
                    subtitle = "Votantes Seleccionados (${voters.size})",
                    voters = voters,
                    format = format
                )
            }
            onResult(result?.first, voters.size, format.mimeType)
        }
    }

    fun exportSelectedVotersToVcf(
        onResult: (Uri?, Int) -> Unit
    ) {
        val ids = _selectedVoterIds.value.toList()
        if (ids.isEmpty()) {
            onResult(null, 0)
            return
        }
        viewModelScope.launch {
            val voters = repository.getVotersByIds(ids)
            val votersWithPhone = voters.filter { it.phone.trim().isNotBlank() }
            if (votersWithPhone.isEmpty()) {
                onResult(null, 0)
                return@launch
            }
            val config = appConfig.value
            val result = withContext(Dispatchers.IO) {
                VCardExporter.generateVotersVcf(
                    context = getApplication(),
                    voters = votersWithPhone,
                    categoryOrGroup = config.appTitle,
                    fileNamePrefix = "Contactos_Seleccionados"
                )
            }
            onResult(result?.first, result?.second ?: 0)
        }
    }

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _passwordChangeSuccess = MutableStateFlow<Boolean?>(null)
    val passwordChangeSuccess: StateFlow<Boolean?> = _passwordChangeSuccess.asStateFlow()

    private val _passwordChangeMessage = MutableStateFlow<String?>(null)
    val passwordChangeMessage: StateFlow<String?> = _passwordChangeMessage.asStateFlow()

    fun login(usernameInput: String, passwordInput: String) {
        val u = usernameInput.trim()
        val p = passwordInput.trim()
        if (u.isBlank()) {
            _loginError.value = "Por favor ingrese su usuario."
            return
        }
        if (p.isBlank()) {
            _loginError.value = "Por favor ingrese su contraseña."
            return
        }

        viewModelScope.launch {
            _isLogging.value = true
            _loginError.value = null
            _sessionBlockedMessage.value = null

            val result = authRepository.login(u, p)
            _loginResult.value = result
            _isLogging.value = false

            when (result) {
                is LoginResult.Success -> {
                    _isLoggedIn.value = true
                    _loginError.value = null
                    _sessionBlockedMessage.value = null
                    attachSessionListener(result.username)
                    startSessionTimer()
                }
                is LoginResult.SessionBlocked -> {
                    _sessionBlockedMessage.value = result.message
                }
                is LoginResult.Error -> {
                    _loginError.value = result.message
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLogging.value = true
            val username = authRepository.getSavedUsername()
            val result = authRepository.logout(username)
            _logoutResult.value = result
            _isLogging.value = false

            when (result) {
                is LogoutResult.Success -> {
                    detachSessionListener()
                    stopSessionTimer()
                    _isLoggedIn.value = false
                    _noInternetLogoutMessage.value = null
                    _showTimeoutNoInternetDialog.value = false
                    _loginError.value = null
                }
                is LogoutResult.NoInternet -> {
                    _noInternetLogoutMessage.value = result.message
                }
                is LogoutResult.Error -> {
                    _loginError.value = result.message
                }
            }
        }
    }

    fun retryLogout() {
        logout()
    }

    fun dismissNoInternetLogout() {
        _noInternetLogoutMessage.value = null
    }

    fun dismissSessionBlockedDialog() {
        _sessionBlockedMessage.value = null
    }

    fun dismissSessionRevokedDialog() {
        _sessionRevokedMessage.value = null
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun setCustomDatabaseUrl(url: String?) {
        authRepository.setCustomDatabaseUrl(url)
    }

    fun getCustomDatabaseUrl(): String? {
        return authRepository.getCustomDatabaseUrl()
    }

    fun changePassword(adminCode: String, newPass: String, confirmPass: String): Boolean {
        if (adminCode.isBlank()) {
            _passwordChangeSuccess.value = false
            _passwordChangeMessage.value = "Debe ingresar el código de administrador."
            return false
        }
        if (newPass.isBlank()) {
            _passwordChangeSuccess.value = false
            _passwordChangeMessage.value = "La nueva contraseña no puede estar vacía."
            return false
        }
        if (newPass != confirmPass) {
            _passwordChangeSuccess.value = false
            _passwordChangeMessage.value = "Las contraseñas no coinciden."
            return false
        }
        val isUpdated = repository.changePasswordWithAdminCode(adminCode, newPass)
        if (isUpdated) {
            _passwordChangeSuccess.value = true
            _passwordChangeMessage.value = "¡Contraseña actualizada con éxito!"
        } else {
            _passwordChangeSuccess.value = false
            _passwordChangeMessage.value = "Código de administrador incorrecto."
        }
        return isUpdated
    }

    fun clearPasswordChangeStatus() {
        _passwordChangeSuccess.value = null
        _passwordChangeMessage.value = null
    }

    val totalVotersCount: StateFlow<Int> = repository.totalVoterCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    val totalVotedCount: StateFlow<Int> = repository.totalVotedCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    val totalVotersWithPhoneCount: StateFlow<Int> = repository.totalVotersWithPhoneCount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    val votingPlaces: StateFlow<List<String>> = repository.votingPlaces.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val tableNumbers: StateFlow<List<String>> = repository.tableNumbers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val citiesOrZones: StateFlow<List<String>> = repository.citiesOrZones.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allBarriosAndAddresses: StateFlow<List<String>> = repository.allBarriosAndAddresses.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val customBarrios: StateFlow<List<String>> = repository.customBarrios

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredVoters: StateFlow<List<Voter>> = combine(
        _searchQuery,
        _searchInputMode,
        _filterState,
        voterGroups
    ) { query, mode, filter, groups ->
        FilterParams(query, mode, filter, groups)
    }.flatMapLatest { params ->
        val query = params.query
        val mode = params.searchMode
        val filter = params.filter
        val groups = params.groups

        val groupVoterIds = if (filter.selectedGroupIds.isNotEmpty()) {
            groups.filter { filter.selectedGroupIds.contains(it.id) }
                .flatMap { it.voterIds }
                .toSet()
        } else {
            null
        }

        val effectiveVoted = when (filter.customPreset) {
            "PENDING_WITH_PHONE" -> false
            "CONFIRMED_VOTED" -> true
            else -> filter.votedFilter
        }

        val effectiveHasPhone = when (filter.customPreset) {
            "PENDING_WITH_PHONE" -> true
            else -> filter.hasPhoneFilter
        }

        val effectiveNotes = if (filter.customPreset == "HAS_NOTES" && filter.notesKeyword.isBlank()) {
            " "
        } else {
            filter.notesKeyword
        }

        repository.searchAndFilterVoters(
            query = query,
            searchMode = mode,
            votingPlaces = filter.selectedVotingPlaces,
            tableNumbers = filter.selectedTableNumbers,
            citiesOrZones = filter.selectedCitiesOrZones,
            barrios = filter.selectedBarrios,
            votedFilter = effectiveVoted,
            hasPhoneFilter = effectiveHasPhone,
            notesKeyword = effectiveNotes,
            selectedGroupVoterIds = groupVoterIds,
            orderNumberFrom = filter.orderNumberFrom,
            orderNumberTo = filter.orderNumberTo
        )
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun setSearchInputMode(mode: SearchInputMode) {
        _searchInputMode.value = mode
    }

    fun toggleVotingPlaceFilter(place: String) {
        val clean = place.trim()
        if (clean.isBlank()) {
            _filterState.value = _filterState.value.copy(selectedVotingPlaces = emptySet())
            return
        }
        val current = _filterState.value.selectedVotingPlaces
        val updated = if (current.contains(clean)) current - clean else current + clean
        _filterState.value = _filterState.value.copy(selectedVotingPlaces = updated)
    }

    fun onVotingPlaceFilterChange(place: String) {
        toggleVotingPlaceFilter(place)
    }

    fun toggleTableFilter(table: String) {
        val clean = table.trim()
        if (clean.isBlank()) {
            _filterState.value = _filterState.value.copy(selectedTableNumbers = emptySet())
            return
        }
        val current = _filterState.value.selectedTableNumbers
        val updated = if (current.contains(clean)) current - clean else current + clean
        _filterState.value = _filterState.value.copy(selectedTableNumbers = updated)
    }

    fun onTableFilterChange(table: String) {
        toggleTableFilter(table)
    }

    fun toggleCityOrZoneFilter(cityOrZone: String) {
        val clean = cityOrZone.trim()
        if (clean.isBlank()) {
            _filterState.value = _filterState.value.copy(selectedCitiesOrZones = emptySet())
            return
        }
        val current = _filterState.value.selectedCitiesOrZones
        val updated = if (current.contains(clean)) current - clean else current + clean
        _filterState.value = _filterState.value.copy(selectedCitiesOrZones = updated)
    }

    fun onCityOrZoneFilterChange(cityOrZone: String) {
        toggleCityOrZoneFilter(cityOrZone)
    }

    fun toggleBarrioFilter(barrio: String) {
        val clean = barrio.trim()
        if (clean.isBlank()) {
            _filterState.value = _filterState.value.copy(selectedBarrios = emptySet())
            return
        }
        val current = _filterState.value.selectedBarrios
        val updated = if (current.contains(clean)) current - clean else current + clean
        _filterState.value = _filterState.value.copy(selectedBarrios = updated)
    }

    fun onAddressOrBarrioFilterChange(addressOrBarrio: String) {
        toggleBarrioFilter(addressOrBarrio)
    }

    fun addCustomBarrio(barrio: String) {
        val clean = barrio.trim()
        if (clean.isNotBlank()) {
            repository.addCustomBarrio(clean)
            _filterState.value = _filterState.value.copy(
                selectedBarrios = _filterState.value.selectedBarrios + clean
            )
        }
    }

    fun removeCustomBarrio(barrio: String) {
        val clean = barrio.trim()
        repository.removeCustomBarrio(clean)
        _filterState.value = _filterState.value.copy(
            selectedBarrios = _filterState.value.selectedBarrios - clean
        )
    }

    fun onVotedFilterChange(voted: Boolean?) {
        _filterState.value = _filterState.value.copy(votedFilter = voted, customPreset = null)
    }

    fun onHasPhoneFilterChange(hasPhone: Boolean?) {
        _filterState.value = _filterState.value.copy(hasPhoneFilter = hasPhone, customPreset = null)
    }

    fun toggleGroupFilter(groupId: String) {
        val current = _filterState.value.selectedGroupIds
        val updated = if (current.contains(groupId)) current - groupId else current + groupId
        _filterState.value = _filterState.value.copy(selectedGroupIds = updated)
    }

    fun onGroupFilterChange(groupId: String?) {
        if (groupId == null) {
            _filterState.value = _filterState.value.copy(selectedGroupIds = emptySet())
        } else {
            toggleGroupFilter(groupId)
        }
    }

    fun onNotesKeywordFilterChange(keyword: String) {
        _filterState.value = _filterState.value.copy(notesKeyword = keyword, customPreset = null)
    }

    fun onOrderNumberRangeChange(from: String, to: String) {
        _filterState.value = _filterState.value.copy(orderNumberFrom = from, orderNumberTo = to)
    }

    fun onApplyCustomPreset(preset: String?) {
        if (preset == null) {
            _filterState.value = _filterState.value.copy(customPreset = null)
        } else {
            when (preset) {
                "PENDING_WITH_PHONE" -> {
                    _filterState.value = _filterState.value.copy(
                        customPreset = "PENDING_WITH_PHONE",
                        votedFilter = false,
                        hasPhoneFilter = true
                    )
                }
                "CONFIRMED_VOTED" -> {
                    _filterState.value = _filterState.value.copy(
                        customPreset = "CONFIRMED_VOTED",
                        votedFilter = true
                    )
                }
                "HAS_NOTES" -> {
                    _filterState.value = _filterState.value.copy(
                        customPreset = "HAS_NOTES"
                    )
                }
                else -> {
                    _filterState.value = _filterState.value.copy(customPreset = preset)
                }
            }
        }
    }

    fun clearSpecificFilter(filterKey: String, specificValue: String? = null) {
        when (filterKey) {
            "voting_place" -> {
                if (specificValue != null) {
                    _filterState.value = _filterState.value.copy(
                        selectedVotingPlaces = _filterState.value.selectedVotingPlaces - specificValue
                    )
                } else {
                    _filterState.value = _filterState.value.copy(selectedVotingPlaces = emptySet())
                }
            }
            "table" -> {
                if (specificValue != null) {
                    _filterState.value = _filterState.value.copy(
                        selectedTableNumbers = _filterState.value.selectedTableNumbers - specificValue
                    )
                } else {
                    _filterState.value = _filterState.value.copy(selectedTableNumbers = emptySet())
                }
            }
            "city" -> {
                if (specificValue != null) {
                    _filterState.value = _filterState.value.copy(
                        selectedCitiesOrZones = _filterState.value.selectedCitiesOrZones - specificValue
                    )
                } else {
                    _filterState.value = _filterState.value.copy(selectedCitiesOrZones = emptySet())
                }
            }
            "barrio" -> {
                if (specificValue != null) {
                    _filterState.value = _filterState.value.copy(
                        selectedBarrios = _filterState.value.selectedBarrios - specificValue
                    )
                } else {
                    _filterState.value = _filterState.value.copy(selectedBarrios = emptySet())
                }
            }
            "voted" -> _filterState.value = _filterState.value.copy(votedFilter = null, customPreset = null)
            "phone" -> _filterState.value = _filterState.value.copy(hasPhoneFilter = null, customPreset = null)
            "group" -> {
                if (specificValue != null) {
                    _filterState.value = _filterState.value.copy(
                        selectedGroupIds = _filterState.value.selectedGroupIds - specificValue
                    )
                } else {
                    _filterState.value = _filterState.value.copy(selectedGroupIds = emptySet())
                }
            }
            "notes" -> _filterState.value = _filterState.value.copy(notesKeyword = "", customPreset = null)
            "order" -> _filterState.value = _filterState.value.copy(orderNumberFrom = "", orderNumberTo = "")
            "preset" -> _filterState.value = _filterState.value.copy(customPreset = null)
        }
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _filterState.value = FilterState()
    }

    fun toggleVotedStatus(voter: Voter) {
        viewModelScope.launch {
            val updated = voter.copy(voted = !voter.voted, updatedAt = System.currentTimeMillis())
            repository.updateVoter(updated)
            if (_selectedVoter.value?.id == voter.id) {
                _selectedVoter.value = updated
            }
        }
    }

    fun selectVoter(voter: Voter?) {
        _selectedVoter.value = voter
    }

    fun openEditVoterDialog(voter: Voter?) {
        _voterToEdit.value = voter
    }

    fun openAddVoterDialog() {
        _isAddVoterDialogOpen.value = true
    }

    fun closeAddVoterDialog() {
        _isAddVoterDialogOpen.value = false
    }

    fun saveVoter(voter: Voter) {
        viewModelScope.launch {
            if (voter.id == 0L) {
                repository.insertVoter(voter)
            } else {
                repository.updateVoter(voter)
            }
            _voterToEdit.value = null
            _isAddVoterDialogOpen.value = false
            if (_selectedVoter.value?.id == voter.id) {
                _selectedVoter.value = voter
            }
        }
    }

    fun deleteVoter(voter: Voter) {
        viewModelScope.launch {
            repository.deleteVoter(voter)
            if (_selectedVoter.value?.id == voter.id) {
                _selectedVoter.value = null
            }
            _voterToEdit.value = null
        }
    }

    fun parseSelectedFile(fileUri: Uri) {
        parseSelectedFiles(listOf(fileUri))
    }

    fun parseSelectedFiles(fileUris: List<Uri>) {
        if (fileUris.isEmpty()) return
        viewModelScope.launch {
            _importState.value = ImportState(isImporting = true)
            val combinedVoters = mutableListOf<Voter>()
            var totalLines = 0
            val combinedHeaders = mutableSetOf<String>()
            val errorMessages = mutableListOf<String>()

            withContext(Dispatchers.IO) {
                for (uri in fileUris) {
                    val result = XlsCsvParser.parseUri(getApplication(), uri)
                    combinedVoters.addAll(result.voters)
                    totalLines += result.totalLinesProcessed
                    combinedHeaders.addAll(result.headersFound)
                    if (result.errorMessage != null) {
                        errorMessages.add(result.errorMessage)
                    }
                }
            }

            val statusMsg = if (errorMessages.isNotEmpty()) {
                "Se procesaron ${fileUris.size} archivo(s). ${combinedVoters.size} registros encontrados. " + errorMessages.joinToString("; ")
            } else {
                "Se analizaron ${fileUris.size} archivo(s) (.xlsx/.csv). ${combinedVoters.size} registros listos para importar."
            }

            _importState.value = ImportState(
                isImporting = false,
                message = statusMsg,
                previewVoters = combinedVoters,
                totalProcessedLines = totalLines,
                headersFound = combinedHeaders.toList()
            )
        }
    }

    fun confirmImportParsedVoters(replaceExisting: Boolean) {
        viewModelScope.launch {
            val votersToImport = _importState.value.previewVoters
            if (votersToImport.isEmpty()) return@launch

            _importState.value = _importState.value.copy(isImporting = true)
            withContext(Dispatchers.IO) {
                if (replaceExisting) {
                    repository.clearAllVoters()
                    repository.insertVoters(votersToImport)
                } else {
                    repository.insertOrUpdateVotersPreservingData(votersToImport)
                }
            }
            _importState.value = ImportState(
                isImporting = false,
                message = if (replaceExisting) {
                    "¡Se reemplazó la base de datos con ${votersToImport.size} registros!"
                } else {
                    "¡Se actualizaron ${votersToImport.size} votantes conservando datos modificados (votos, teléfonos y notas)!"
                },
                previewVoters = emptyList()
            )
        }
    }

    fun loadSampleDataset() {
        viewModelScope.launch {
            _importState.value = ImportState(isImporting = true)
            val sample = SampleDataProvider.getSampleVoters()
            withContext(Dispatchers.IO) {
                repository.insertVoters(sample)
            }
            _importState.value = ImportState(
                isImporting = false,
                message = "Se cargó el padrón de muestra (${sample.size} registros) exitosamente."
            )
        }
    }

    fun clearDatabase() {
        viewModelScope.launch {
            repository.clearAllVoters()
            _importState.value = ImportState(
                message = "Base de datos local limpiada correctamente."
            )
        }
    }

    fun updateAppTitle(newTitle: String) {
        repository.updateAppTitle(newTitle)
    }

    fun updateFlyerUri(uriString: String?) {
        repository.updateFlyerUri(uriString)
    }

    fun updatePrimaryColor(colorHex: String) {
        repository.updatePrimaryColor(colorHex)
    }

    fun updateThemeMode(mode: ThemeMode) {
        repository.updateThemeMode(mode)
    }

    fun exportFilteredToPdf(customSubtitle: String) {
        viewModelScope.launch {
            val voters = filteredVoters.value
            val config = appConfig.value
            val uri = withContext(Dispatchers.IO) {
                PdfReportExporter.generateVotersPdf(
                    context = getApplication(),
                    reportTitle = config.appTitle,
                    subtitle = customSubtitle,
                    primaryColorHex = config.primaryColorHex,
                    voters = voters
                )
            }
            if (uri != null) {
                _exportResult.value = ExportResult(
                    fileUri = uri,
                    mimeType = "application/pdf",
                    message = "Reporte PDF generado exitosamente con ${voters.size} registros."
                )
            } else {
                _exportResult.value = ExportResult(
                    message = "Ocurrió un error al generar el archivo PDF."
                )
            }
        }
    }

    fun exportFilteredToExcel(format: ExcelFormat = ExcelFormat.XLSX, customSubtitle: String = "") {
        viewModelScope.launch {
            val voters = filteredVoters.value
            val config = appConfig.value
            val result = withContext(Dispatchers.IO) {
                ExcelReportExporter.generateVotersSpreadsheet(
                    context = getApplication(),
                    reportTitle = config.appTitle,
                    subtitle = customSubtitle.ifBlank { "Padrón Electoral Oficial" },
                    voters = voters,
                    format = format
                )
            }
            if (result?.first != null) {
                _exportResult.value = ExportResult(
                    fileUri = result.first,
                    mimeType = result.second,
                    message = "Planilla ${format.displayName} generada exitosamente con ${voters.size} registros."
                )
            } else {
                _exportResult.value = ExportResult(
                    message = "Ocurrió un error al generar la planilla ${format.displayName}."
                )
            }
        }
    }

    /**
     * Exports ALL voters in the entire database that have a phone number to a .vcf contact file.
     */
    fun exportAllWithPhoneToVcf(customCategory: String = "") {
        viewModelScope.launch {
            val votersWithPhone = withContext(Dispatchers.IO) {
                repository.getVotersWithPhoneList()
            }
            if (votersWithPhone.isEmpty()) {
                _exportResult.value = ExportResult(
                    message = "No se encontraron votantes con número de teléfono registrado en el padrón."
                )
                return@launch
            }
            val config = appConfig.value
            val category = customCategory.ifBlank { config.appTitle }
            val result = withContext(Dispatchers.IO) {
                VCardExporter.generateVotersVcf(
                    context = getApplication(),
                    voters = votersWithPhone,
                    categoryOrGroup = category,
                    fileNamePrefix = "Contactos_Todos_Padron"
                )
            }
            if (result?.first != null) {
                _exportResult.value = ExportResult(
                    fileUri = result.first,
                    mimeType = VCardExporter.VCF_MIME_TYPE,
                    message = "¡Archivo .vcf generado con éxito! ${result.second} contactos listos para importar a tu teléfono o WhatsApp."
                )
            } else {
                _exportResult.value = ExportResult(
                    message = "Ocurrió un error al generar el archivo de contactos .vcf."
                )
            }
        }
    }

    /**
     * Exports the filtered list of voters that have a phone number to a .vcf contact file.
     */
    fun exportFilteredToVcf(customCategory: String = "") {
        viewModelScope.launch {
            val votersWithPhone = filteredVoters.value.filter { it.phone.trim().isNotBlank() }
            if (votersWithPhone.isEmpty()) {
                _exportResult.value = ExportResult(
                    message = "No hay votantes con teléfono dentro de la selección o filtros actuales."
                )
                return@launch
            }
            val config = appConfig.value
            val category = customCategory.ifBlank { config.appTitle }
            val result = withContext(Dispatchers.IO) {
                VCardExporter.generateVotersVcf(
                    context = getApplication(),
                    voters = votersWithPhone,
                    categoryOrGroup = category,
                    fileNamePrefix = "Contactos_Filtrados"
                )
            }
            if (result?.first != null) {
                _exportResult.value = ExportResult(
                    fileUri = result.first,
                    mimeType = VCardExporter.VCF_MIME_TYPE,
                    message = "¡Archivo .vcf generado con éxito! ${result.second} contactos filtrados listos para importar o compartir."
                )
            } else {
                _exportResult.value = ExportResult(
                    message = "Ocurrió un error al generar el archivo de contactos .vcf."
                )
            }
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }
}
