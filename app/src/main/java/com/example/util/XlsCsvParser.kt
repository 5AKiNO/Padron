package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.local.Voter
import java.io.BufferedReader
import java.io.StringReader
import java.nio.charset.Charset
import java.util.regex.Pattern

object XlsCsvParser {

    data class ParseResult(
        val voters: List<Voter>,
        val totalLinesProcessed: Int,
        val headersFound: List<String>,
        val errorMessage: String? = null
    )

    fun parseUri(context: Context, uri: Uri): ParseResult {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
                ?: return ParseResult(emptyList(), 0, emptyList(), "No se pudo abrir el archivo seleccionado.")

            val bytes = inputStream.readBytes()
            inputStream.close()

            if (bytes.isEmpty()) {
                return ParseResult(emptyList(), 0, emptyList(), "El archivo seleccionado está vacío.")
            }

            val contentString = TextEncodingSanitizer.decodeBytesSafely(bytes)

            val reader = BufferedReader(StringReader(contentString))
            val lines = mutableListOf<String>()
            var line: String? = reader.readLine()
            var count = 0
            while (line != null && count < 20000) { // Safety limit 20k rows per file
                if (line.isNotBlank()) {
                    lines.add(line)
                }
                line = reader.readLine()
                count++
            }
            reader.close()

            if (lines.isEmpty()) {
                return ParseResult(emptyList(), 0, emptyList(), "El archivo seleccionado está vacío.")
            }

            val rawContent = lines.joinToString("\n")
            if (rawContent.contains("<table", ignoreCase = true) || rawContent.contains("<tr>", ignoreCase = true)) {
                return parseHtmlXls(rawContent)
            }

            parseCsvOrTsv(lines)
        } catch (e: Exception) {
            ParseResult(emptyList(), 0, emptyList(), "Error al leer el archivo: ${e.localizedMessage}")
        }
    }

    private fun parseCsvOrTsv(lines: List<String>): ParseResult {
        val firstLine = lines.first()
        val delimiter = when {
            firstLine.contains("\t") -> "\t"
            firstLine.contains(";") -> ";"
            firstLine.contains(",") -> ","
            else -> ";"
        }

        val rawHeaders = splitLine(firstLine, delimiter).map { cleanCell(it) }
        val headerIndices = mapHeaders(rawHeaders)

        val voters = mutableListOf<Voter>()
        val startIdx = if (headerIndices.hasNameOrCedula) 1 else 0

        for (i in startIdx until lines.size) {
            val row = splitLine(lines[i], delimiter)
            if (row.isEmpty()) continue

            val voter = extractVoterFromRow(row, headerIndices, i)
            if (voter.fullName.isNotBlank() || voter.cedula.isNotBlank()) {
                voters.add(voter)
            }
        }

        return ParseResult(
            voters = voters,
            totalLinesProcessed = lines.size,
            headersFound = rawHeaders
        )
    }

    private fun parseHtmlXls(html: String): ParseResult {
        val voters = mutableListOf<Voter>()
        val rowMatcher = Pattern.compile("(?i)<tr[^>]*>(.*?)</tr>", Pattern.DOTALL).matcher(html)

        val rows = mutableListOf<List<String>>()
        while (rowMatcher.find()) {
            val trContent = rowMatcher.group(1) ?: ""
            val cellMatcher = Pattern.compile("(?i)<t[dh][^>]*>(.*?)</t[dh]>", Pattern.DOTALL).matcher(trContent)
            val rowCells = mutableListOf<String>()
            while (cellMatcher.find()) {
                val rawText = cellMatcher.group(1) ?: ""
                val clean = rawText.replace(Regex("(?i)<[^>]*>"), "").replace("&nbsp;", " ").trim()
                rowCells.add(clean)
            }
            if (rowCells.isNotEmpty()) {
                rows.add(rowCells)
            }
        }

        if (rows.isEmpty()) {
            return ParseResult(emptyList(), 0, emptyList(), "No se encontraron filas estructuradas en el archivo XLS.")
        }

        val headers = rows.first().map { cleanCell(it) }
        val headerIndices = mapHeaders(headers)
        val startIdx = if (headerIndices.hasNameOrCedula) 1 else 0

        for (i in startIdx until rows.size) {
            val row = rows[i]
            val voter = extractVoterFromRow(row, headerIndices, i)
            if (voter.fullName.isNotBlank() || voter.cedula.isNotBlank()) {
                voters.add(voter)
            }
        }

        return ParseResult(
            voters = voters,
            totalLinesProcessed = rows.size,
            headersFound = headers
        )
    }

    private fun splitLine(line: String, delimiter: String): List<String> {
        val tokens = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()

        for (ch in line.toCharArray()) {
            if (ch == '"') {
                inQuotes = !inQuotes
            } else if (ch.toString() == delimiter && !inQuotes) {
                tokens.add(sb.toString())
                sb.clear()
            } else {
                sb.append(ch)
            }
        }
        tokens.add(sb.toString())
        return tokens
    }

    private fun cleanCell(value: String): String {
        val trimmed = value.trim().removeSurrounding("\"", "\"").trim()
        return TextEncodingSanitizer.sanitizeText(trimmed)
    }

    private class HeaderIndices(
        var cedulaIdx: Int = -1,
        var nameIdx: Int = -1,
        var votingPlaceIdx: Int = -1,
        var tableIdx: Int = -1,
        var orderIdx: Int = -1,
        var addressIdx: Int = -1,
        var cityIdx: Int = -1,
        var phoneIdx: Int = -1,
        var notesIdx: Int = -1,
        var votedIdx: Int = -1
    ) {
        val hasNameOrCedula: Boolean get() = nameIdx != -1 || cedulaIdx != -1
    }

    private fun mapHeaders(headers: List<String>): HeaderIndices {
        val idxMap = HeaderIndices()

        headers.forEachIndexed { idx, h ->
            val norm = h.lowercase().replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")

            when {
                idxMap.cedulaIdx == -1 && (norm.contains("cedula") || norm.contains("ci") || norm.contains("documento") || norm.contains("dni") || norm.contains("identidad")) -> {
                    idxMap.cedulaIdx = idx
                }
                idxMap.nameIdx == -1 && (norm.contains("nombre") || norm.contains("apellido") || norm.contains("votante") || norm.contains("electores") || norm.contains("persona")) -> {
                    idxMap.nameIdx = idx
                }
                idxMap.votingPlaceIdx == -1 && (norm.contains("lugar") || norm.contains("escuela") || norm.contains("colegio") || norm.contains("local") || norm.contains("puesto") || norm.contains("recinto")) -> {
                    idxMap.votingPlaceIdx = idx
                }
                idxMap.tableIdx == -1 && (norm.contains("mesa")) -> {
                    idxMap.tableIdx = idx
                }
                idxMap.orderIdx == -1 && (norm.contains("orden") || norm.contains("nro") || norm.contains("num")) -> {
                    idxMap.orderIdx = idx
                }
                idxMap.addressIdx == -1 && (norm.contains("direccion") || norm.contains("domicilio") || norm.contains("calle")) -> {
                    idxMap.addressIdx = idx
                }
                idxMap.cityIdx == -1 && (norm.contains("ciudad") || norm.contains("departamento") || norm.contains("distrito") || norm.contains("barrio") || norm.contains("zona")) -> {
                    idxMap.cityIdx = idx
                }
                idxMap.phoneIdx == -1 && (norm.contains("telefono") || norm.contains("celular") || norm.contains("contacto")) -> {
                    idxMap.phoneIdx = idx
                }
                idxMap.notesIdx == -1 && (norm.contains("nota") || norm.contains("observac") || norm.contains("comentario")) -> {
                    idxMap.notesIdx = idx
                }
                idxMap.votedIdx == -1 && (norm.contains("voto") || norm.contains("estado") || norm.contains("asistencia")) -> {
                    idxMap.votedIdx = idx
                }
            }
        }

        // Fallback default mapping if headers were not recognized
        if (!idxMap.hasNameOrCedula && headers.size >= 2) {
            idxMap.cedulaIdx = 0
            idxMap.nameIdx = 1
            if (headers.size > 2) idxMap.votingPlaceIdx = 2
            if (headers.size > 3) idxMap.tableIdx = 3
            if (headers.size > 4) idxMap.orderIdx = 4
        }

        return idxMap
    }

    private fun extractVoterFromRow(row: List<String>, idxMap: HeaderIndices, rowLineNum: Int): Voter {
        fun getVal(idx: Int): String {
            if (idx >= 0 && idx < row.size) {
                return cleanCell(row[idx])
            }
            return ""
        }

        val cedula = getVal(idxMap.cedulaIdx)
        val name = getVal(idxMap.nameIdx)
        val votingPlace = getVal(idxMap.votingPlaceIdx)
        val table = getVal(idxMap.tableIdx)
        val order = getVal(idxMap.orderIdx)
        val address = getVal(idxMap.addressIdx)
        val city = getVal(idxMap.cityIdx)
        val phone = getVal(idxMap.phoneIdx)
        val notes = getVal(idxMap.notesIdx)
        val votedStr = getVal(idxMap.votedIdx)
        val voted = votedStr.equals("si", ignoreCase = true) || votedStr.equals("true", ignoreCase = true) || votedStr.equals("1", ignoreCase = true) || votedStr.contains("voto", ignoreCase = true)

        return Voter(
            cedula = cedula.ifBlank { "SIN-ID-$rowLineNum" },
            fullName = name.ifBlank { "Votante Desconocido $rowLineNum" },
            votingPlace = votingPlace,
            tableNumber = table,
            orderNumber = order,
            address = address,
            cityOrZone = city,
            phone = phone,
            notes = notes,
            voted = voted
        )
    }
}
