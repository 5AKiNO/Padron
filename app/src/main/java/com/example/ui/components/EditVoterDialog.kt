package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.Voter

@Composable
fun EditVoterDialog(
    voter: Voter?,
    onDismiss: () -> Unit,
    onSave: (Voter) -> Unit
) {
    var cedula by remember { mutableStateOf(voter?.cedula ?: "") }
    var fullName by remember { mutableStateOf(voter?.fullName ?: "") }
    var votingPlace by remember { mutableStateOf(voter?.votingPlace ?: "") }
    var tableNumber by remember { mutableStateOf(voter?.tableNumber ?: "") }
    var orderNumber by remember { mutableStateOf(voter?.orderNumber ?: "") }
    var address by remember { mutableStateOf(voter?.address ?: "") }
    var phone by remember { mutableStateOf(voter?.phone ?: "") }
    var notes by remember { mutableStateOf(voter?.notes ?: "") }
    var voted by remember { mutableStateOf(voter?.voted ?: false) }

    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (voter == null || voter.id == 0L) "Agregar Nuevo Votante" else "Editar Votante",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Nombre Completo *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    isError = isError && fullName.isBlank(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth().testTag("edit_fullname_input")
                )

                OutlinedTextField(
                    value = cedula,
                    onValueChange = {
                        cedula = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Cédula de Identidad *") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    isError = isError && cedula.isBlank(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("edit_cedula_input")
                )

                OutlinedTextField(
                    value = votingPlace,
                    onValueChange = { votingPlace = it },
                    label = { Text("Lugar de Votación (Escuela / Colegio)") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_votingplace_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tableNumber,
                        onValueChange = { tableNumber = it },
                        label = { Text("Mesa") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_table_input")
                    )
                    OutlinedTextField(
                        value = orderNumber,
                        onValueChange = { orderNumber = it },
                        label = { Text("Nº Orden") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_order_input")
                    )
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Dirección / Barrio") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_address_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono de Contacto") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("edit_phone_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observaciones / Notas") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_notes_input")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Checkbox(
                        checked = voted,
                        onCheckedChange = { voted = it },
                        modifier = Modifier.testTag("edit_voted_checkbox")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ya ejerció el voto (Registrado)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isBlank() || cedula.isBlank()) {
                        isError = true
                    } else {
                        onSave(
                            Voter(
                                id = voter?.id ?: 0L,
                                cedula = cedula.trim(),
                                fullName = fullName.trim(),
                                votingPlace = votingPlace.trim(),
                                tableNumber = tableNumber.trim(),
                                orderNumber = orderNumber.trim(),
                                address = address.trim(),
                                phone = phone.trim(),
                                notes = notes.trim(),
                                voted = voted,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_voter_button")
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_voter_button")
            ) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
