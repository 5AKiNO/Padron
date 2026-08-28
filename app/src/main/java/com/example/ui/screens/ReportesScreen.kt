package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PermContactCalendar
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.Voter
import com.example.data.preferences.AppConfig
import com.example.ui.ExportResult
import com.example.util.ExcelFormat
import com.example.util.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(
    appConfig: AppConfig,
    filteredVoters: List<Voter>,
    totalVotersCount: Int = 0,
    totalWithPhoneCount: Int = 0,
    exportResult: ExportResult?,
    onExportPdf: (customSubtitle: String) -> Unit,
    onExportExcel: (format: ExcelFormat, customSubtitle: String) -> Unit,
    onExportAllVcf: (customCategory: String) -> Unit,
    onExportFilteredVcf: (customCategory: String) -> Unit,
    onClearExportResult: () -> Unit
) {
    var subtitleNote by remember { mutableStateOf("Reporte Filtrado - Padrón Oficial") }
    var contactCategory by remember { mutableStateOf(appConfig.appTitle) }
    val context = LocalContext.current

    val votedCount = filteredVoters.count { it.voted }
    val pendingCount = filteredVoters.size - votedCount
    val filteredWithPhoneCount = filteredVoters.count { it.phone.trim().isNotBlank() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exportar y Reportes", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Assessment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Resumen de Registros",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Padrón total: $totalVotersCount | Selección activa: ${filteredVoters.size} votantes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ReportMetricBox(
                            title = "Filtrados",
                            value = "${filteredVoters.size}",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        ReportMetricBox(
                            title = "Con Teléfono",
                            value = "$totalWithPhoneCount",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        ReportMetricBox(
                            title = "Ya Votaron",
                            value = "$votedCount",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        ReportMetricBox(
                            title = "Pendientes",
                            value = "$pendingCount",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Export Result Notification Card & Share Action
            if (exportResult != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = exportResult.message,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        if (exportResult.fileUri != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        if (exportResult.mimeType == "text/x-vcard" || exportResult.mimeType.contains("vcard")) {
                                            ShareUtils.shareVcfFile(context, exportResult.fileUri, "Compartir Contactos (.vcf)")
                                        } else if (exportResult.mimeType == "application/pdf") {
                                            ShareUtils.sharePdfFile(context, exportResult.fileUri, "Compartir Reporte PDF")
                                        } else {
                                            ShareUtils.shareSpreadsheetFile(context, exportResult.fileUri, exportResult.mimeType, "Compartir Planilla")
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("share_export_button")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Compartir")
                                }

                                Button(
                                    onClick = {
                                        if (exportResult.mimeType == "text/x-vcard" || exportResult.mimeType.contains("vcard")) {
                                            ShareUtils.openVcfFile(context, exportResult.fileUri)
                                        } else {
                                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(exportResult.fileUri, exportResult.mimeType)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            try {
                                                context.startActivity(Intent.createChooser(viewIntent, "Abrir con"))
                                            } catch (e: Exception) {
                                                // Fallback
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.weight(1f).testTag("open_export_button")
                                ) {
                                    Text("Abrir / Importar")
                                }
                            }
                        }
                    }
                }
            }

            // EXPORT VCF CONTACTS CARD (USER REQUEST)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PermContactCalendar,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Exportar Contactos (.vcf)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                            Text(
                                text = "Exporta los nombres que tienen número de teléfono para agregarlos a WhatsApp o a tu libreta de contactos del teléfono.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Votantes con teléfono en todo el padrón:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF0284C7)
                            ) {
                                Text(
                                    text = "$totalWithPhoneCount contactos",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = contactCategory,
                        onValueChange = { contactCategory = it },
                        label = { Text("Etiqueta / Categoría de los Contactos") },
                        leadingIcon = { Icon(Icons.Default.Contacts, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vcf_category_input"),
                        singleLine = true
                    )

                    // Button 1: Export ALL voters with phone
                    Button(
                        onClick = { onExportAllVcf(contactCategory) },
                        enabled = totalWithPhoneCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_all_vcf_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Exportar TODOS con Teléfono ($totalWithPhoneCount en .vcf)",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Button 2: Export FILTERED voters with phone
                    if (filteredVoters.size != totalVotersCount) {
                        OutlinedButton(
                            onClick = { onExportFilteredVcf(contactCategory) },
                            enabled = filteredWithPhoneCount > 0,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_filtered_vcf_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Exportar Filtro Actual con Teléfono ($filteredWithPhoneCount en .vcf)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Customization Options for Documents & Spreadsheets
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Configuración de Documentos y Planillas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = subtitleNote,
                        onValueChange = { subtitleNote = it },
                        label = { Text("Subtítulo / Nota del Reporte") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_subtitle_input"),
                        singleLine = true
                    )
                }
            }

            // Export Actions (PDF, XLSX, XLS, CSV)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Exportar Documentos y Planillas (${filteredVoters.size} registros)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Export PDF
                    Button(
                        onClick = { onExportPdf(subtitleNote) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_pdf_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar a Documento PDF (.pdf)", fontWeight = FontWeight.Bold)
                    }

                    // Export XLSX
                    Button(
                        onClick = { onExportExcel(ExcelFormat.XLSX, subtitleNote) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_xlsx_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar a Planilla Excel (.xlsx)", fontWeight = FontWeight.Bold)
                    }

                    // Export XLS
                    Button(
                        onClick = { onExportExcel(ExcelFormat.XLS, subtitleNote) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_xls_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.TableView, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar a Planilla Excel (.xls)", fontWeight = FontWeight.Bold)
                    }

                    // Export CSV
                    OutlinedButton(
                        onClick = { onExportExcel(ExcelFormat.CSV, subtitleNote) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_csv_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar a Archivo Delimitado (.csv)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportMetricBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
