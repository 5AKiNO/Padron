package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.Voter
import com.example.data.preferences.AppConfig
import com.example.ui.FilterState
import com.example.util.ExcelFormat
import com.example.util.ShareUtils

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.util.SearchInputMode

@Composable
fun BuscadorScreen(
    appConfig: AppConfig,
    searchQuery: String,
    searchInputMode: SearchInputMode = SearchInputMode.CEDULA,
    filterState: FilterState,
    voters: List<Voter>,
    totalCount: Int,
    votedCount: Int,
    isSelectionMode: Boolean,
    selectedVoterIds: Set<Long>,
    voterGroups: List<com.example.data.model.VoterGroup> = emptyList(),
    groupsCount: Int,
    onQueryChange: (String) -> Unit,
    onSearchInputModeChange: (SearchInputMode) -> Unit = {},
    onOpenFilterSheet: () -> Unit,
    onOpenGroupsSheet: () -> Unit,
    onClearFilterKey: (String, String?) -> Unit = { _, _ -> },
    onSelectVoter: (Voter) -> Unit,
    onToggleVoted: (Voter) -> Unit,
    onEnterSelectionMode: (Long?) -> Unit,
    onExitSelectionMode: () -> Unit,
    onToggleSelectVoter: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onOpenCreateGroup: () -> Unit,
    onExportSelectedPdf: () -> Unit,
    onExportSelectedExcel: (ExcelFormat) -> Unit,
    onExportSelectedVcf: () -> Unit = {},
    onBulkMarkVotante: (Boolean) -> Unit,
    onAddVoterClick: () -> Unit,
    onGoToImportClick: () -> Unit
) {
    val context = LocalContext.current
    val activeFiltersCount = filterState.activeCount()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Flyer Banner Header
            BannerHeader(
                appConfig = appConfig,
                groupsCount = groupsCount,
                onOpenGroupsSheet = onOpenGroupsSheet
            )

            // Search Bar & Filter Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Search Mode Selector: 1° Nº de Cédula, 2° Nombre y Apellido
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1º Opción: Nº de Cédula
                    FilterChip(
                        selected = searchInputMode == SearchInputMode.CEDULA,
                        onClick = { onSearchInputModeChange(SearchInputMode.CEDULA) },
                        label = {
                            Text(
                                "🔢 Nº de Cédula",
                                fontWeight = if (searchInputMode == SearchInputMode.CEDULA) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            if (searchInputMode == SearchInputMode.CEDULA) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_mode_cedula_chip"),
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    // 2º Opción: Nombre y Apellido (Aproximado)
                    FilterChip(
                        selected = searchInputMode == SearchInputMode.NOMBRE_APELLIDO,
                        onClick = { onSearchInputModeChange(SearchInputMode.NOMBRE_APELLIDO) },
                        label = {
                            Text(
                                "🔤 Nombre y Apellido",
                                fontWeight = if (searchInputMode == SearchInputMode.NOMBRE_APELLIDO) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            if (searchInputMode == SearchInputMode.NOMBRE_APELLIDO) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_mode_name_chip"),
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input_field"),
                    placeholder = {
                        Text(
                            if (searchInputMode == SearchInputMode.CEDULA)
                                "Ingrese Nº de Cédula (teclado numérico)..."
                            else
                                "Buscar por Nombre o Apellido (rápido y aproximado)..."
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (searchInputMode == SearchInputMode.CEDULA) KeyboardType.Number else KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = if (searchInputMode == SearchInputMode.CEDULA) Icons.Default.Numbers else Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                            IconButton(
                                onClick = onOpenFilterSheet,
                                modifier = Modifier.testTag("open_filters_icon_button")
                            ) {
                                if (activeFiltersCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text(activeFiltersCount.toString())
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.FilterAlt,
                                            contentDescription = "Filtros Activos",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    Icon(
                                        Icons.Default.FilterList,
                                        contentDescription = "Filtros",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )

                // Active Filters Chips Row (Supports multiple simultaneous filters)
                if (activeFiltersCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Preset chip
                        if (!filterState.customPreset.isNullOrBlank()) {
                            val presetLabel = when (filterState.customPreset) {
                                "PENDING_WITH_PHONE" -> "⚡ Pendientes con Teléfono"
                                "CONFIRMED_VOTED" -> "⚡ Confirmados Votaron"
                                "HAS_NOTES" -> "⚡ Con Notas"
                                else -> "⚡ ${filterState.customPreset}"
                            }
                            InputChip(
                                selected = true,
                                onClick = { onClearFilterKey("preset", null) },
                                label = { Text(presetLabel, fontSize = 11.sp) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Quitar filtro", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        // Group chips (Multiple selected groups)
                        filterState.selectedGroupIds.forEach { groupId ->
                            val groupName = voterGroups.find { it.id == groupId }?.name ?: "Grupo"
                            InputChip(
                                selected = true,
                                onClick = { onClearFilterKey("group", groupId) },
                                label = { Text("Grupo: $groupName", fontSize = 11.sp) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Quitar filtro", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        // Voted chip
                        if (filterState.votedFilter != null && filterState.customPreset == null) {
                            InputChip(
                                selected = true,
                                onClick = { onClearFilterKey("voted", null) },
                                label = { Text(if (filterState.votedFilter == true) "Ya Votó" else "Pendiente", fontSize = 11.sp) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Quitar filtro", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        // Phone chip
                        if (filterState.hasPhoneFilter != null && filterState.customPreset == null) {
                            InputChip(
                                selected = true,
                                onClick = { onClearFilterKey("phone", null) },
                                label = { Text(if (filterState.hasPhoneFilter == true) "Con Teléfono" else "Sin Teléfono", fontSize = 11.sp) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Quitar filtro", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        // Voting place chips (Multiple selected places)
                        filterState.selectedVotingPlaces.forEach { place ->
                            InputChip(
                                selected = true,
                                onClick = { onClearFilterKey("voting_place", place) },
                                label = { Text("Lugar: $place", fontSize = 11.sp, maxLines = 1) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Quitar lugar", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        // Table chips (Multiple selected tables)
                        filterState.selectedTableNumbers.forEach { table ->
                            InputChip(
                                selected = true,
                                onClick = { onClearFilterKey("table", table) },
                                label = { Text("Mesa: $table", fontSize = 11.sp) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Quitar mesa", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        // City chips (Multiple selected cities/zones)
                        filterState.selectedCitiesOrZones.forEach { city ->
                            InputChip(
                                selected = true,
                                onClick = { onClearFilterKey("city", city) },
                                label = { Text("Zona: $city", fontSize = 11.sp) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Quitar zona", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        // Barrio / Address chips (Multiple selected barrios)
                        filterState.selectedBarrios.forEach { barrio ->
                            InputChip(
                                selected = true,
                                onClick = { onClearFilterKey("barrio", barrio) },
                                label = { Text("Barrio: $barrio", fontSize = 11.sp) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Quitar barrio", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        // Notes keyword chip
                        if (filterState.notesKeyword.isNotBlank() && filterState.customPreset == null) {
                            InputChip(
                                selected = true,
                                onClick = { onClearFilterKey("notes", null) },
                                label = { Text("Nota: '${filterState.notesKeyword}'", fontSize = 11.sp) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Quitar filtro", modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        // Order Range chip
                        if (filterState.orderNumberFrom.isNotBlank() || filterState.orderNumberTo.isNotBlank()) {
                            InputChip(
                                selected = true,
                                onClick = { onClearFilterKey("order", null) },
                                label = { Text("Orden: ${filterState.orderNumberFrom.ifBlank { "1" }} - ${filterState.orderNumberTo.ifBlank { "fin" }}", fontSize = 11.sp) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Quitar filtro", modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stats & Action Bar / Mode Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Padrón: $totalCount", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )

                        AssistChip(
                            onClick = { },
                            label = { Text("Votantes: $votedCount", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF166534).copy(alpha = 0.12f),
                                labelColor = Color(0xFF15803D)
                            )
                        )
                    }

                    // Button to toggle selection mode or view groups
                    if (!isSelectionMode && voters.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = { onEnterSelectionMode(null) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("enter_selection_mode_button")
                        ) {
                            Icon(Icons.Default.CheckBox, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Seleccionar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Contextual Selection Action Bar when in Selection Mode
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onExitSelectionMode) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancelar Selección")
                                }
                                Text(
                                    text = "${selectedVoterIds.size} de ${voters.size} seleccionados",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row {
                                if (selectedVoterIds.size < voters.size) {
                                    OutlinedButton(
                                        onClick = onSelectAll,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Todos", fontSize = 11.sp)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = onClearSelection,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Limpiar", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Action Buttons for Selected Voters
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Row 1: WhatsApp and PDF
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // WhatsApp Share Button
                                Button(
                                    onClick = {
                                        val selectedList = voters.filter { selectedVoterIds.contains(it.id) }
                                        if (selectedList.isEmpty()) {
                                            Toast.makeText(context, "Seleccione al menos un votante", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val text = ShareUtils.formatVotersForWhatsApp(
                                                title = "Votantes Seleccionados (${selectedList.size})",
                                                voters = selectedList
                                            )
                                            ShareUtils.shareViaWhatsApp(context, text)
                                        }
                                    },
                                    enabled = selectedVoterIds.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("bulk_whatsapp_button")
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                // Export to PDF Button
                                Button(
                                    onClick = onExportSelectedPdf,
                                    enabled = selectedVoterIds.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("bulk_export_pdf_button")
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Exportar PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            // Row 2: Excel XLSX and XLS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onExportSelectedExcel(ExcelFormat.XLSX) },
                                    enabled = selectedVoterIds.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("bulk_export_xlsx_button")
                                ) {
                                    Icon(Icons.Default.TableChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Excel (.xlsx)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { onExportSelectedExcel(ExcelFormat.XLS) },
                                    enabled = selectedVoterIds.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("bulk_export_xls_button")
                                ) {
                                    Icon(Icons.Default.TableView, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Excel (.xls)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = onExportSelectedVcf,
                                    enabled = selectedVoterIds.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("bulk_export_vcf_button")
                                ) {
                                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("VCF (.vcf)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            // Row 3: Create Group and Bulk Mark
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Create Group Button
                                Button(
                                    onClick = onOpenCreateGroup,
                                    enabled = selectedVoterIds.isNotEmpty(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("bulk_create_group_button")
                                ) {
                                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Crear Grupo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                // Bulk Mark as Votante
                                Button(
                                    onClick = { onBulkMarkVotante(true) },
                                    enabled = selectedVoterIds.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("bulk_mark_votante_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Votaron", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // Bulk Unmark / Pendiente
                                Button(
                                    onClick = { onBulkMarkVotante(false) },
                                    enabled = selectedVoterIds.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("bulk_unmark_votante_button")
                                ) {
                                    Icon(Icons.Default.CheckBoxOutlineBlank, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Pendiente", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Voter List or Empty State
            if (voters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = if (totalCount == 0) Icons.Default.UploadFile else Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (totalCount == 0) "El padrón local está vacío" else "No se encontraron votantes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (totalCount == 0)
                                "Cargue su base de datos en formato .xlsx, .xls o .csv desde la pestaña 'Cargar Padrón'."
                            else
                                "Intente cambiar la cédula ingresada o borre los filtros.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (totalCount == 0) {
                            Button(onClick = onGoToImportClick) {
                                Icon(Icons.Default.UploadFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ir a Cargar Padrón")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("voter_list_lazy_column"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = voters,
                        key = { it.id }
                    ) { voter ->
                        val isSelected = selectedVoterIds.contains(voter.id)
                        VoterItemCard(
                            voter = voter,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelectionMode) {
                                    onToggleSelectVoter(voter.id)
                                } else {
                                    onSelectVoter(voter)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    onEnterSelectionMode(voter.id)
                                } else {
                                    onToggleSelectVoter(voter.id)
                                }
                            },
                            onToggleVoted = { onToggleVoted(voter) },
                            onToggleSelection = { onToggleSelectVoter(voter.id) }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add New Voter Manually
        if (!isSelectionMode) {
            FloatingActionButton(
                onClick = onAddVoterClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("add_voter_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Votante")
            }
        }
    }
}

@Composable
private fun BannerHeader(
    appConfig: AppConfig,
    groupsCount: Int,
    onOpenGroupsSheet: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(MaterialTheme.colorScheme.primary)
    ) {
        // Banner Image or Fallback Asset
        if (appConfig.flyerUri != null) {
            AsyncImage(
                model = Uri.parse(appConfig.flyerUri),
                contentDescription = "Flyer personalizado",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_flyer_default),
                contentDescription = "Banner predeterminado",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Overlay Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.50f))
        )

        // Title and Groups Button Overlay
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appConfig.appTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Padrón Electoral Oficial • Consulta de Cédulas",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            // Quick access to Voter Groups
            Surface(
                onClick = onOpenGroupsSheet,
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.testTag("open_groups_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = "Grupos",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Grupos ($groupsCount)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VoterItemCard(
    voter: Voter,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleVoted: () -> Unit,
    onToggleSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("voter_item_card_${voter.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection Checkbox in selection mode OR Person/Votante icon in normal mode
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (voter.voted) Color(0xFF166534).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (voter.voted) Icons.Default.CheckCircle else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (voter.voted) Color(0xFF15803D) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                // APELLIDO Y NOMBRE (Highlighted)
                Text(
                    text = voter.fullName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // ORDEN
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "ORDEN: ${if (voter.orderNumber.isNotBlank()) voter.orderNumber else "-"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // CI
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "CI: ${voter.cedula}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (voter.tableNumber.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Mesa ${voter.tableNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // LOCAL
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LOCAL: ${if (voter.votingPlace.isNotBlank()) voter.votingPlace else "Sin asignar"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick Toggle Checkbox for Votante
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Checkbox(
                    checked = voter.voted,
                    onCheckedChange = { onToggleVoted() },
                    modifier = Modifier.testTag("toggle_voted_checkbox_${voter.id}")
                )
                Text(
                    text = if (voter.voted) "Votante" else "Pendiente",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = if (voter.voted) FontWeight.Bold else FontWeight.Normal,
                    color = if (voter.voted) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
