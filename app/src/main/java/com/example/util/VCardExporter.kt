package com.example.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.Voter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VCardExporter {

    const val VCF_MIME_TYPE = "text/x-vcard"

    /**
     * Generates a standard vCard (.vcf) file containing all voters with a non-blank phone number.
     * Compatible with Android Contacts, Google Contacts, iOS, WhatsApp, Telegram, Outlook, etc.
     *
     * @return Pair of (File Uri, Total exported contacts count) or null if no voters have phones.
     */
    fun generateVotersVcf(
        context: Context,
        voters: List<Voter>,
        categoryOrGroup: String = "Padrón Electoral 2026",
        fileNamePrefix: String = "Contactos_Padron"
    ): Pair<Uri?, Int>? {
        val votersWithPhone = voters.filter { it.phone.trim().isNotBlank() }
        if (votersWithPhone.isEmpty()) {
            return null
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${fileNamePrefix}_${timeStamp}.vcf"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        return try {
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    for (voter in votersWithPhone) {
                        val vcard = formatVCardEntry(voter, categoryOrGroup)
                        writer.write(vcard)
                    }
                    writer.flush()
                }
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Pair(uri, votersWithPhone.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Formats a single voter into a standard vCard 3.0 record.
     */
    fun formatVCardEntry(voter: Voter, defaultCategory: String): String {
        val cleanFullName = TextEncodingSanitizer.sanitizeText(voter.fullName).replace("\n", " ").replace("\r", "")
        val cleanPhone = voter.phone.trim().replace("\n", "").replace("\r", "")
        val cleanCedula = TextEncodingSanitizer.sanitizeText(voter.cedula)
        val cleanPlace = TextEncodingSanitizer.sanitizeText(voter.votingPlace)
        val cleanTable = TextEncodingSanitizer.sanitizeText(voter.tableNumber)
        val cleanOrder = TextEncodingSanitizer.sanitizeText(voter.orderNumber)
        val cleanAddress = TextEncodingSanitizer.sanitizeText(voter.address)
        val cleanCity = TextEncodingSanitizer.sanitizeText(voter.cityOrZone)
        val cleanNotes = TextEncodingSanitizer.sanitizeText(voter.notes)
        val statusText = if (voter.voted) "Votó" else "Pendiente"

        // Build descriptive Note field containing all relevant electoral and log details
        val noteParts = mutableListOf<String>()
        if (cleanCedula.isNotBlank()) noteParts.add("Cédula: $cleanCedula")
        if (cleanPlace.isNotBlank()) noteParts.add("Local: $cleanPlace")
        if (cleanTable.isNotBlank()) noteParts.add("Mesa: $cleanTable")
        if (cleanOrder.isNotBlank()) noteParts.add("Nº Orden: $cleanOrder")
        noteParts.add("Estado: $statusText")
        if (cleanCity.isNotBlank()) noteParts.add("Zona/Ciudad: $cleanCity")
        if (cleanAddress.isNotBlank()) noteParts.add("Dirección: $cleanAddress")
        if (cleanNotes.isNotBlank()) noteParts.add("Notas: $cleanNotes")

        val noteString = escapeVCardValue(noteParts.joinToString(" | "))

        // Parse structured name (Family;Given)
        val structuredName = parseStructuredName(cleanFullName)

        val sb = StringBuilder()
        sb.append("BEGIN:VCARD\r\n")
        sb.append("VERSION:3.0\r\n")
        sb.append("FN:${escapeVCardValue(cleanFullName)}\r\n")
        sb.append("N:${escapeVCardValue(structuredName.first)};${escapeVCardValue(structuredName.second)};;;\r\n")
        sb.append("TEL;TYPE=CELL,VOICE:${cleanPhone}\r\n")
        
        if (defaultCategory.isNotBlank()) {
            sb.append("ORG:${escapeVCardValue(defaultCategory)}\r\n")
            sb.append("CATEGORIES:${escapeVCardValue(defaultCategory)}\r\n")
        }
        
        if (cleanAddress.isNotBlank() || cleanCity.isNotBlank()) {
            sb.append("ADR;TYPE=HOME:;;${escapeVCardValue(cleanAddress)};${escapeVCardValue(cleanCity)};;;;\r\n")
        }
        
        if (noteString.isNotBlank()) {
            sb.append("NOTE:${noteString}\r\n")
        }
        sb.append("END:VCARD\r\n")

        return sb.toString()
    }

    private fun escapeVCardValue(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
            .replace("\r", "")
    }

    private fun parseStructuredName(fullName: String): Pair<String, String> {
        val tokens = fullName.split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            tokens.isEmpty() -> Pair("", "")
            tokens.size == 1 -> Pair("", tokens[0])
            tokens.size == 2 -> Pair(tokens[1], tokens[0]) // Family, Given
            else -> {
                // If 3 or more words, take first 1-2 as given and the rest as family
                val splitIdx = if (tokens.size >= 4) 2 else 1
                val given = tokens.take(splitIdx).joinToString(" ")
                val family = tokens.drop(splitIdx).joinToString(" ")
                Pair(family, given)
            }
        }
    }
}
