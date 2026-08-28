package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VoterGroup
import com.example.ui.FilterState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    filterState: FilterState,
    votingPlaces: List<String>,
    tableNumbers: List<String>,
    citiesOrZones: List<String>,
    allBarriosAndAddresses: List<String> = emptyList(),
    customBarrios: List<String> = emptyList(),
    voterGroups: List<VoterGroup>,
    onDismiss: () -> Unit,
    onVotingPlaceSelect: (String) -> Unit,
    onTableSelect: (String) -> Unit,
    onCityOrZoneSelect: (String) -> Unit,
    onAddressOrBarrioSelect: (String) -> Unit,
    onAddCustomBarrio: (String) -> Unit,
    onRemoveCustomBarrio: (String) -> Unit,
    onVotedSelect: (Boolean?) -> Unit,
    onHasPhoneSelect: (Boolean?) -> Unit,
    onGroupSelect: (String?) -> Unit,
    onNotesKeywordChange: (String) -> Unit,
    onOrderNumberRangeChange: (from: String, to: String) -> Unit,
    onApplyCustomPreset: (String?) -> Unit,
    onClearAll: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("filter_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Filtros Múltiples",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val active = filterState.activeCount()
                        Text(
                            text = if (active > 0) "$active filtro(s) activo(s) simultáneamente" else "Seleccione uno o más filtros",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (active > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onClearAll,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("clear_filters_button")
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Limpiar Todo", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. FILTROS RÁPIDOS INTELIGENTES / PRESETS
            Text(
                text = "⚡ Filtros Rápidos",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filterState.customPreset == "PENDING_WITH_PHONE",
                    onClick = {
                        if (filterState.customPreset == "PENDING_WITH_PHONE") onApplyCustomPreset(null)
                        else onApplyCustomPreset("PENDING_WITH_PHONE")
                    },
                    leadingIcon = if (filterState.customPreset == "PENDING_WITH_PHONE") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    label = { Text("Pendientes con Teléfono 📞") },
                    modifier = Modifier.testTag("preset_pending_phone")
                )

                FilterChip(
                    selected = filterState.customPreset == "CONFIRMED_VOTED",
                    onClick = {
                        if (filterState.customPreset == "CONFIRMED_VOTED") onApplyCustomPreset(null)
                        else onApplyCustomPreset("CONFIRMED_VOTED")
                    },
                    leadingIcon = if (filterState.customPreset == "CONFIRMED_VOTED") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    label = { Text("Votantes Confirmados ✅") },
                    modifier = Modifier.testTag("preset_confirmed_voted")
                )

                FilterChip(
                    selected = filterState.customPreset == "HAS_NOTES",
                    onClick = {
                        if (filterState.customPreset == "HAS_NOTES") onApplyCustomPreset(null)
                        else onApplyCustomPreset("HAS_NOTES")
                    },
                    leadingIcon = if (filterState.customPreset == "HAS_NOTES") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    label = { Text("Con Notas / Observaciones 📝") },
                    modifier = Modifier.testTag("preset_has_notes")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // 2. FILTRO POR GRUPO DE VOTANTES (SELECCIÓN MÚLTIPLE)
            if (voterGroups.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Filtrar por Grupo(s)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (filterState.selectedGroupIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${filterState.selectedGroupIds.size} seleccionados)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filterState.selectedGroupIds.isEmpty(),
                        onClick = { onGroupSelect(null) },
                        label = { Text("Todos los grupos") }
                    )
                    voterGroups.forEach { group ->
                        val isSelected = filterState.selectedGroupIds.contains(group.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onGroupSelect(group.id) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            label = { Text("${group.name} (${group.voterIds.size})") },
                            modifier = Modifier.testTag("filter_group_${group.id}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 3. ESTADO DE ASISTENCIA
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Estado de Asistencia",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterState.votedFilter == null && filterState.customPreset == null,
                    onClick = { onVotedSelect(null) },
                    label = { Text("Todos") },
                    modifier = Modifier.testTag("filter_voted_all")
                )
                FilterChip(
                    selected = filterState.votedFilter == true,
                    onClick = { onVotedSelect(if (filterState.votedFilter == true) null else true) },
                    leadingIcon = if (filterState.votedFilter == true) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    label = { Text("Ya Votó") },
                    modifier = Modifier.testTag("filter_voted_yes")
                )
                FilterChip(
                    selected = filterState.votedFilter == false,
                    onClick = { onVotedSelect(if (filterState.votedFilter == false) null else false) },
                    leadingIcon = if (filterState.votedFilter == false) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    label = { Text("Pendiente") },
                    modifier = Modifier.testTag("filter_voted_no")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. DISPONIBILIDAD DE TELÉFONO
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Disponibilidad de Teléfono",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterState.hasPhoneFilter == null && filterState.customPreset == null,
                    onClick = { onHasPhoneSelect(null) },
                    label = { Text("Todos") },
                    modifier = Modifier.testTag("filter_phone_all")
                )
                FilterChip(
                    selected = filterState.hasPhoneFilter == true,
                    onClick = { onHasPhoneSelect(if (filterState.hasPhoneFilter == true) null else true) },
                    leadingIcon = if (filterState.hasPhoneFilter == true) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    label = { Text("Con Teléfono") },
                    modifier = Modifier.testTag("filter_phone_with")
                )
                FilterChip(
                    selected = filterState.hasPhoneFilter == false,
                    onClick = { onHasPhoneSelect(if (filterState.hasPhoneFilter == false) null else false) },
                    leadingIcon = if (filterState.hasPhoneFilter == false) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    label = { Text("Sin Teléfono") },
                    modifier = Modifier.testTag("filter_phone_without")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // 5. LUGAR DE VOTACIÓN (SELECCIÓN MÚLTIPLE)
            if (votingPlaces.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lugar(es) de Votación",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (filterState.selectedVotingPlaces.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${filterState.selectedVotingPlaces.size} seleccionados)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filterState.selectedVotingPlaces.isEmpty(),
                        onClick = { onVotingPlaceSelect("") },
                        label = { Text("Todos los Lugares") }
                    )
                    votingPlaces.forEach { place ->
                        val isSelected = filterState.selectedVotingPlaces.contains(place)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onVotingPlaceSelect(place) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            label = { Text(place) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 6. NÚMERO DE MESA (SELECCIÓN MÚLTIPLE)
            if (tableNumbers.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TableRestaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Número(s) de Mesa",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (filterState.selectedTableNumbers.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${filterState.selectedTableNumbers.size} seleccionadas)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filterState.selectedTableNumbers.isEmpty(),
                        onClick = { onTableSelect("") },
                        label = { Text("Todas las Mesas") }
                    )
                    tableNumbers.forEach { table ->
                        val isSelected = filterState.selectedTableNumbers.contains(table)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTableSelect(table) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            label = { Text("Mesa $table") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 7. CIUDAD / ZONA (SELECCIÓN MÚLTIPLE)
            if (citiesOrZones.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ciudad(es) / Zona(s)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (filterState.selectedCitiesOrZones.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${filterState.selectedCitiesOrZones.size} seleccionadas)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filterState.selectedCitiesOrZones.isEmpty(),
                        onClick = { onCityOrZoneSelect("") },
                        label = { Text("Todas las Zonas") }
                    )
                    citiesOrZones.forEach { city ->
                        val isSelected = filterState.selectedCitiesOrZones.contains(city)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCityOrZoneSelect(city) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            label = { Text(city) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 8. DIRECCIÓN / BARRIO (SELECCIÓN MÚLTIPLE Y AGREGAR PERSONALIZADOS)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Dirección / Barrio(s)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (filterState.selectedBarrios.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${filterState.selectedBarrios.size} seleccionados)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            var newBarrioInput by remember { mutableStateOf("") }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newBarrioInput,
                    onValueChange = { newBarrioInput = it },
                    placeholder = { Text("Escriba un barrio o dirección...") },
                    leadingIcon = {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (newBarrioInput.isNotBlank()) {
                            IconButton(onClick = { newBarrioInput = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Borrar", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("barrio_filter_text_field")
                )

                Button(
                    onClick = {
                        val toAdd = newBarrioInput.trim()
                        if (toAdd.isNotBlank()) {
                            onAddCustomBarrio(toAdd)
                            newBarrioInput = ""
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    enabled = newBarrioInput.isNotBlank(),
                    modifier = Modifier.testTag("add_custom_barrio_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agregar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Chips list of available and custom added barrios
            val combinedBarrios = remember(allBarriosAndAddresses, customBarrios, filterState.selectedBarrios) {
                (customBarrios + allBarriosAndAddresses + filterState.selectedBarrios)
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
            }

            if (combinedBarrios.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filterState.selectedBarrios.isEmpty(),
                        onClick = { onAddressOrBarrioSelect("") },
                        label = { Text("Todos los Barrios") }
                    )
                    combinedBarrios.forEach { barrio ->
                        val isSelected = filterState.selectedBarrios.contains(barrio)
                        val isCustom = customBarrios.any { it.equals(barrio, ignoreCase = true) }
                        FilterChip(
                            selected = isSelected,
                            onClick = { onAddressOrBarrioSelect(barrio) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            label = { Text(barrio) },
                            trailingIcon = if (isCustom) {
                                {
                                    IconButton(
                                        onClick = { onRemoveCustomBarrio(barrio) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Eliminar de favoritos", modifier = Modifier.size(12.dp))
                                    }
                                }
                            } else null,
                            modifier = Modifier.testTag("filter_barrio_chip_$barrio")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // 9. RANGO DE NÚMERO DE ORDEN EN PADRÓN
            Text(
                text = "Rango de Nº de Orden (Padrón)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = filterState.orderNumberFrom,
                    onValueChange = { onOrderNumberRangeChange(it, filterState.orderNumberTo) },
                    label = { Text("Desde Nº") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = filterState.orderNumberTo,
                    onValueChange = { onOrderNumberRangeChange(filterState.orderNumberFrom, it) },
                    label = { Text("Hasta Nº") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 10. FILTRO POR PALABRA CLAVE EN NOTAS / OBSERVACIONES
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Palabra Clave en Notas / Observaciones",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = filterState.notesKeyword,
                onValueChange = onNotesKeywordChange,
                placeholder = { Text("Ej: Líder, Transporte, Móvil, Testigo...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 11. BOTÓN APLICAR FILTROS
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_filters_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                val count = filterState.activeCount()
                Text(
                    text = if (count > 0) "Aplicar Filtros ($count activos)" else "Aplicar Filtros",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
