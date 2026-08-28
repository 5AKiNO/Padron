package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.VoterGroup
import com.example.ui.components.CreateGroupDialog
import com.example.ui.components.EditGroupDialog
import com.example.ui.components.EditVoterDialog
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.GruposBottomSheet
import com.example.ui.components.VoterDetailSheet
import com.example.ui.screens.BuscadorScreen
import com.example.ui.screens.ConfiguracionScreen
import com.example.ui.screens.ImportScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ReportesScreen
import com.example.util.ShareUtils

enum class MainTab(val title: String, val icon: ImageVector, val tag: String) {
    BUSCADOR("Buscador", Icons.Default.Search, "tab_buscador"),
    IMPORTAR("Cargar Padrón", Icons.Default.CloudUpload, "tab_importar"),
    REPORTES("Reportes", Icons.Default.Assessment, "tab_reportes"),
    CONFIGURACION("Configuración", Icons.Default.Settings, "tab_configuracion")
}

@Composable
fun MainScreen(viewModel: VoterViewModel = viewModel()) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val loginError by viewModel.loginError.collectAsState()
    val passwordChangeSuccess by viewModel.passwordChangeSuccess.collectAsState()
    val passwordChangeMessage by viewModel.passwordChangeMessage.collectAsState()

    val appConfig by viewModel.appConfig.collectAsState()
    val context = LocalContext.current

    val isLogging by viewModel.isLogging.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val sessionBlockedMessage by viewModel.sessionBlockedMessage.collectAsState()
    val sessionRevokedMessage by viewModel.sessionRevokedMessage.collectAsState()
    val noInternetLogoutMessage by viewModel.noInternetLogoutMessage.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val isLimitedUser by viewModel.isLimitedUser.collectAsState()
    val showTimeoutNoInternetDialog by viewModel.showTimeoutNoInternetDialog.collectAsState()
    val currentDeviceId = viewModel.currentDeviceId

    if (!isLoggedIn) {
        LoginScreen(
            appTitle = appConfig.appTitle,
            errorMessage = loginError,
            isLoading = isLogging,
            sessionBlockedMessage = sessionBlockedMessage,
            sessionRevokedMessage = sessionRevokedMessage,
            deviceId = currentDeviceId,
            isOnline = isOnline,
            onLogin = { user, pass -> viewModel.login(user, pass) },
            onClearError = viewModel::clearLoginError,
            onDismissBlockedDialog = viewModel::dismissSessionBlockedDialog,
            onDismissRevokedDialog = viewModel::dismissSessionRevokedDialog
        )
        return
    }

    // Non-dismissible Dialog when attempting manual logout without internet
    if (noInternetLogoutMessage != null) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss when offline */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            icon = {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Sin conexión a Internet",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = noInternetLogoutMessage ?: "Sin conexión a Internet. Conéctate a una red para notificar la desactivación de tu sesión.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::retryLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reintentar Desactivación", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::dismissNoInternetLogout
                ) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Non-dismissible Dialog when 10-minute session expires and there is no internet
    if (showTimeoutNoInternetDialog) {
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss until connection is restored */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            icon = {
                Icon(
                    imageVector = Icons.Default.TimerOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "TIEMPO AGOTADO",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                val trialMinutes = viewModel.trialMinutes
                val timeLabel = if (trialMinutes % 60L == 0L && trialMinutes >= 60L) {
                    val hours = trialMinutes / 60L
                    if (hours == 1L) "1 hora" else "$hours horas"
                } else {
                    "$trialMinutes min"
                }
                Text(
                    text = "Tu tiempo de uso ($timeLabel) ha expirado. Para liberar tu sesión y cerrar la app correctamente, DEBES CONECTARTE A INTERNET.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::retryTimeoutLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("REINTENTAR CONEXIÓN", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    var selectedTab by remember { mutableStateOf(MainTab.BUSCADOR) }
    var isFilterSheetOpen by remember { mutableStateOf(false) }
    var isGroupsSheetOpen by remember { mutableStateOf(false) }
    var isCreateGroupDialogOpen by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<VoterGroup?>(null) }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchInputMode by viewModel.searchInputMode.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val filteredVoters by viewModel.filteredVoters.collectAsState()
    val totalCount by viewModel.totalVotersCount.collectAsState()
    val totalWithPhoneCount by viewModel.totalVotersWithPhoneCount.collectAsState()
    val votedCount by viewModel.totalVotedCount.collectAsState()
    val votingPlaces by viewModel.votingPlaces.collectAsState()
    val tableNumbers by viewModel.tableNumbers.collectAsState()
    val citiesOrZones by viewModel.citiesOrZones.collectAsState()
    val allBarriosAndAddresses by viewModel.allBarriosAndAddresses.collectAsState()
    val customBarrios by viewModel.customBarrios.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val selectedVoter by viewModel.selectedVoter.collectAsState()
    val voterToEdit by viewModel.voterToEdit.collectAsState()
    val isAddVoterDialogOpen by viewModel.isAddVoterDialogOpen.collectAsState()
    val exportResult by viewModel.exportResult.collectAsState()

    val voterGroups by viewModel.voterGroups.collectAsState()
    val selectedVoterIds by viewModel.selectedVoterIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("main_navigation_bar")) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            if (isSelectionMode) viewModel.exitSelectionMode()
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Limited User Session Countdown Bar
            if (isLimitedUser) {
                val minutes = timeLeft / 60
                val seconds = timeLeft % 60
                val isUrgent = timeLeft < 120 // less than 2 minutes remaining

                Surface(
                    color = if (isUrgent) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isUrgent) Icons.Default.TimerOff else Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tiempo restante:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isUrgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
            when (selectedTab) {
                MainTab.BUSCADOR -> BuscadorScreen(
                    appConfig = appConfig,
                    searchQuery = searchQuery,
                    searchInputMode = searchInputMode,
                    filterState = filterState,
                    voters = filteredVoters,
                    totalCount = totalCount,
                    votedCount = votedCount,
                    isSelectionMode = isSelectionMode,
                    selectedVoterIds = selectedVoterIds,
                    voterGroups = voterGroups,
                    groupsCount = voterGroups.size,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onSearchInputModeChange = viewModel::setSearchInputMode,
                    onOpenFilterSheet = { isFilterSheetOpen = true },
                    onOpenGroupsSheet = { isGroupsSheetOpen = true },
                    onClearFilterKey = viewModel::clearSpecificFilter,
                    onSelectVoter = viewModel::selectVoter,
                    onToggleVoted = viewModel::toggleVotedStatus,
                    onEnterSelectionMode = { id -> viewModel.enterSelectionMode(id) },
                    onExitSelectionMode = viewModel::exitSelectionMode,
                    onToggleSelectVoter = viewModel::toggleVoterSelection,
                    onSelectAll = { viewModel.selectAllVisibleVoters(filteredVoters) },
                    onClearSelection = viewModel::clearSelection,
                    onOpenCreateGroup = { isCreateGroupDialogOpen = true },
                    onExportSelectedPdf = {
                        viewModel.exportSelectedVotersToPdf { uri, count ->
                            if (uri != null) {
                                Toast.makeText(context, "PDF generado con $count votantes", Toast.LENGTH_SHORT).show()
                                ShareUtils.sharePdfFile(context, uri, "Compartir Votantes Seleccionados")
                            } else {
                                Toast.makeText(context, "No se pudo generar el archivo PDF", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onExportSelectedExcel = { format ->
                        viewModel.exportSelectedVotersToExcel(format) { uri, count, mimeType ->
                            if (uri != null) {
                                Toast.makeText(context, "Planilla ${format.displayName} generada ($count votantes)", Toast.LENGTH_SHORT).show()
                                ShareUtils.shareSpreadsheetFile(context, uri, mimeType, "Compartir Votantes Seleccionados (${format.extension.uppercase()})")
                            } else {
                                Toast.makeText(context, "No se pudo generar la planilla Excel", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onExportSelectedVcf = {
                        viewModel.exportSelectedVotersToVcf { uri, count ->
                            if (uri != null) {
                                Toast.makeText(context, "Archivo VCF generado ($count contactos con teléfono)", Toast.LENGTH_SHORT).show()
                                ShareUtils.shareVcfFile(context, uri, "Compartir Contactos (.vcf)")
                            } else {
                                Toast.makeText(context, "Ningún votante seleccionado tiene número de teléfono registrado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onBulkMarkVotante = viewModel::bulkMarkSelectedAsVotante,
                    onAddVoterClick = viewModel::openAddVoterDialog,
                    onGoToImportClick = { selectedTab = MainTab.IMPORTAR }
                )

                MainTab.IMPORTAR -> ImportScreen(
                    importState = importState,
                    totalCount = totalCount,
                    onFilesSelected = viewModel::parseSelectedFiles,
                    onConfirmImport = viewModel::confirmImportParsedVoters,
                    onLoadSample = viewModel::loadSampleDataset,
                    onClearDatabase = viewModel::clearDatabase
                )

                MainTab.REPORTES -> ReportesScreen(
                    appConfig = appConfig,
                    filteredVoters = filteredVoters,
                    totalVotersCount = totalCount,
                    totalWithPhoneCount = totalWithPhoneCount,
                    exportResult = exportResult,
                    onExportPdf = viewModel::exportFilteredToPdf,
                    onExportExcel = { format, subtitle ->
                        viewModel.exportFilteredToExcel(format, subtitle)
                    },
                    onExportAllVcf = viewModel::exportAllWithPhoneToVcf,
                    onExportFilteredVcf = viewModel::exportFilteredToVcf,
                    onClearExportResult = viewModel::clearExportResult
                )

                MainTab.CONFIGURACION -> ConfiguracionScreen(
                    appConfig = appConfig,
                    passwordChangeSuccess = passwordChangeSuccess,
                    passwordChangeMessage = passwordChangeMessage,
                    deviceId = currentDeviceId,
                    isOnline = isOnline,
                    customDatabaseUrl = viewModel.getCustomDatabaseUrl(),
                    onUpdateTitle = viewModel::updateAppTitle,
                    onUpdateFlyerUri = viewModel::updateFlyerUri,
                    onUpdatePrimaryColor = viewModel::updatePrimaryColor,
                    onUpdateThemeMode = viewModel::updateThemeMode,
                    onAddVoterClick = viewModel::openAddVoterDialog,
                    onSubmitChangePassword = { code, newPass, confirmPass ->
                        viewModel.changePassword(code, newPass, confirmPass)
                    },
                    onClearPasswordChangeStatus = viewModel::clearPasswordChangeStatus,
                    onUpdateCustomDatabaseUrl = viewModel::setCustomDatabaseUrl,
                    onLogout = viewModel::logout
                )
            }
        }
    }
    }

    // Modal Bottom Sheet for Voter Detail View
    if (selectedVoter != null) {
        VoterDetailSheet(
            voter = selectedVoter,
            onDismiss = { viewModel.selectVoter(null) },
            onToggleVoted = viewModel::toggleVotedStatus,
            onEdit = { voter ->
                viewModel.selectVoter(null)
                viewModel.openEditVoterDialog(voter)
            },
            onDelete = viewModel::deleteVoter
        )
    }

    // Modal Bottom Sheet for Custom Filters
    if (isFilterSheetOpen) {
        FilterBottomSheet(
            filterState = filterState,
            votingPlaces = votingPlaces,
            tableNumbers = tableNumbers,
            citiesOrZones = citiesOrZones,
            allBarriosAndAddresses = allBarriosAndAddresses,
            customBarrios = customBarrios,
            voterGroups = voterGroups,
            onDismiss = { isFilterSheetOpen = false },
            onVotingPlaceSelect = viewModel::onVotingPlaceFilterChange,
            onTableSelect = viewModel::onTableFilterChange,
            onCityOrZoneSelect = viewModel::onCityOrZoneFilterChange,
            onAddressOrBarrioSelect = viewModel::onAddressOrBarrioFilterChange,
            onAddCustomBarrio = viewModel::addCustomBarrio,
            onRemoveCustomBarrio = viewModel::removeCustomBarrio,
            onVotedSelect = viewModel::onVotedFilterChange,
            onHasPhoneSelect = viewModel::onHasPhoneFilterChange,
            onGroupSelect = viewModel::onGroupFilterChange,
            onNotesKeywordChange = viewModel::onNotesKeywordFilterChange,
            onOrderNumberRangeChange = viewModel::onOrderNumberRangeChange,
            onApplyCustomPreset = viewModel::onApplyCustomPreset,
            onClearAll = viewModel::clearFilters
        )
    }

    // Modal Bottom Sheet for Saved Groups
    if (isGroupsSheetOpen) {
        GruposBottomSheet(
            groups = voterGroups,
            allVoters = filteredVoters,
            onDismiss = { isGroupsSheetOpen = false },
            onShareGroupWhatsApp = { group ->
                viewModel.getVotersByIds(group.voterIds) { members ->
                    if (members.isEmpty()) {
                        Toast.makeText(context, "El grupo no contiene votantes válidos", Toast.LENGTH_SHORT).show()
                    } else {
                        val text = ShareUtils.formatVotersForWhatsApp(
                            title = group.name,
                            voters = members
                        )
                        ShareUtils.shareViaWhatsApp(context, text)
                    }
                }
            },
            onExportGroupPdf = { group ->
                viewModel.exportGroupToPdf(group) { uri, members ->
                    if (uri != null) {
                        Toast.makeText(context, "PDF de '${group.name}' generado (${members.size} votantes)", Toast.LENGTH_SHORT).show()
                        ShareUtils.sharePdfFile(context, uri, "Compartir PDF - ${group.name}")
                    } else {
                        Toast.makeText(context, "No se pudo generar el archivo PDF", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onExportGroupExcel = { group, format ->
                viewModel.exportGroupToExcel(group, format) { uri, members, mimeType ->
                    if (uri != null) {
                        Toast.makeText(context, "Planilla ${format.displayName} de '${group.name}' generada (${members.size} votantes)", Toast.LENGTH_SHORT).show()
                        ShareUtils.shareSpreadsheetFile(context, uri, mimeType, "Compartir ${group.name} (${format.extension.uppercase()})")
                    } else {
                        Toast.makeText(context, "No se pudo generar la planilla Excel", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onExportGroupVcf = { group ->
                viewModel.exportGroupToVcf(group) { uri, count ->
                    if (uri != null) {
                        Toast.makeText(context, "Archivo VCF de '${group.name}' generado ($count contactos con teléfono)", Toast.LENGTH_SHORT).show()
                        ShareUtils.shareVcfFile(context, uri, "Compartir Contactos VCF - ${group.name}")
                    } else {
                        Toast.makeText(context, "El grupo '${group.name}' no tiene integrantes con teléfono registrado", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onMarkGroupStatus = { group, voted ->
                viewModel.markGroupVotersStatus(group, voted)
                val statusText = if (voted) "marcados como que ya votaron" else "marcados como pendientes"
                Toast.makeText(context, "Votantes del grupo '${group.name}' $statusText", Toast.LENGTH_SHORT).show()
            },
            onToggleVoterStatus = viewModel::toggleVotedStatus,
            onEditGroup = { group ->
                groupToEdit = group
            },
            onFilterByGroup = { group ->
                viewModel.onGroupFilterChange(group.id)
                Toast.makeText(context, "Filtrando por grupo: '${group.name}'", Toast.LENGTH_SHORT).show()
            },
            onDeleteGroup = { group ->
                viewModel.deleteGroup(group.id)
                Toast.makeText(context, "Grupo eliminado", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog for Editing an Existing Group
    if (groupToEdit != null) {
        EditGroupDialog(
            group = groupToEdit!!,
            allVoters = filteredVoters,
            onDismiss = { groupToEdit = null },
            onSave = { groupId, newName, voterIds ->
                viewModel.updateGroup(groupId, newName, voterIds)
                Toast.makeText(context, "Grupo actualizado correctamente", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog for Creating a Group from Selected Voters
    if (isCreateGroupDialogOpen) {
        CreateGroupDialog(
            selectedCount = selectedVoterIds.size,
            onDismiss = { isCreateGroupDialogOpen = false },
            onConfirmCreate = { name ->
                val group = viewModel.createGroupFromSelection(name)
                isCreateGroupDialogOpen = false
                viewModel.exitSelectionMode()
                if (group != null) {
                    Toast.makeText(context, "¡Grupo '${group.name}' creado con éxito!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Dialog for Adding / Editing Voter
    if (isAddVoterDialogOpen || voterToEdit != null) {
        EditVoterDialog(
            voter = voterToEdit,
            onDismiss = {
                viewModel.closeAddVoterDialog()
                viewModel.openEditVoterDialog(null)
            },
            onSave = viewModel::saveVoter
        )
    }
}
