package com.example.util

import com.example.data.local.Voter
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Robust text encoding sanitizer and multi-charset detector.
 * Resolves mojibake (Ã‘ -> Ñ, Ã± -> ñ, etc.), HTML entities (&Ntilde; -> Ñ, &#209; -> Ñ),
 * Windows-1252/ISO-8859-1/IBM850/UTF-8/UTF-16 encodings, and Unicode replacement characters.
 */
object TextEncodingSanitizer {

    private val WINDOWS_1252: Charset = try {
        Charset.forName("windows-1252")
    } catch (e: Exception) {
        StandardCharsets.ISO_8859_1
    }

    private val IBM850: Charset = try {
        Charset.forName("IBM850")
    } catch (e: Exception) {
        StandardCharsets.ISO_8859_1
    }

    /**
     * Safely decodes raw byte arrays into clean, proper Unicode strings by detecting BOMs
     * and scoring candidate encodings for Spanish language character integrity.
     */
    fun decodeBytesSafely(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""

        // 1. Detect Byte Order Mark (BOM)
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return sanitizeText(String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8))
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return sanitizeText(String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16LE))
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return sanitizeText(String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE))
        }

        // 2. Decode with candidate charsets
        val utf8Candidate = try { String(bytes, StandardCharsets.UTF_8) } catch (e: Exception) { "" }
        val win1252Candidate = try { String(bytes, WINDOWS_1252) } catch (e: Exception) { "" }
        val ibm850Candidate = try { String(bytes, IBM850) } catch (e: Exception) { "" }

        // 3. Score candidate charsets
        val utf8Score = scoreTextQuality(utf8Candidate, isUtf8 = true)
        val win1252Score = scoreTextQuality(win1252Candidate, isUtf8 = false)
        val ibm850Score = scoreTextQuality(ibm850Candidate, isUtf8 = false)

        val bestCandidate = when {
            utf8Score >= win1252Score && utf8Score >= ibm850Score -> utf8Candidate
            win1252Score >= ibm850Score -> win1252Candidate
            else -> ibm850Candidate
        }

        return sanitizeText(bestCandidate)
    }

    private fun scoreTextQuality(text: String, isUtf8: Boolean): Int {
        if (text.isEmpty()) return -1000
        var score = 0

        // Heavy penalty for replacement character or null bytes
        val replacementCount = text.count { it == '\uFFFD' }
        score -= replacementCount * 50

        val nullByteCount = text.count { it == '\u0000' }
        score -= nullByteCount * 100

        // Heavy penalty for mojibake patterns
        val mojibakePatterns = listOf("Ã‘", "Ã±", "Ã", "Ã‰", "Ã", "Ã“", "Ãš", "Ã¡", "Ã©", "Ã­", "Ã³", "Ãº", "Âº", "Âª", "Â¿")
        for (pattern in mojibakePatterns) {
            if (text.contains(pattern)) {
                score -= 30
            }
        }

        // Reward proper Spanish accented letters and Ñ
        val spanishLetters = listOf('Ñ', 'ñ', 'Á', 'á', 'É', 'é', 'Í', 'í', 'Ó', 'ó', 'Ú', 'ú', 'Ü', 'ü')
        for (ch in spanishLetters) {
            score += text.count { it == ch } * 10
        }

        // Reward common Spanish ASCII letters and delimiters
        score += text.count { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == ';' || it == ',' || it == '\n' }

        return score
    }

    /**
     * Sanitizes any text by fixing mojibake, HTML entities, URL encodings,
     * and corrupted replacement characters in Spanish surnames/words (e.g. ACUÑA).
     */
    fun sanitizeText(raw: String): String {
        if (raw.isBlank()) return raw.trim()

        var text = raw

        // Remove BOM if present inside string
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1)
        }

        // 1. Fix Mojibake (UTF-8 bytes misinterpreted as Windows-1252/ISO-8859-1)
        text = text
            // Ñ and ñ
            .replace("Ã‘", "Ñ")
            .replace("Ã±", "ñ")
            .replace("\u00C3\u0091", "Ñ")
            .replace("\u00C3\u00B1", "ñ")
            // Uppercase vowels
            .replace("Ã", "Á")
            .replace("Ã‰", "É")
            .replace("Ã", "Í")
            .replace("Ã“", "Ó")
            .replace("Ãš", "Ú")
            .replace("Ãœ", "Ü")
            .replace("\u00C3\u0081", "Á")
            .replace("\u00C3\u0089", "É")
            .replace("\u00C3\u008D", "Í")
            .replace("\u00C3\u0093", "Ó")
            .replace("\u00C3\u009A", "Ú")
            .replace("\u00C3\u009C", "Ü")
            // Lowercase vowels
            .replace("Ã¡", "á")
            .replace("Ã©", "é")
            .replace("Ã­", "í")
            .replace("Ã³", "ó")
            .replace("Ãº", "ú")
            .replace("Ã¼", "ü")
            .replace("\u00C3\u00A1", "á")
            .replace("\u00C3\u00A9", "é")
            .replace("\u00C3\u00AD", "í")
            .replace("\u00C3\u00B3", "ó")
            .replace("\u00C3\u00BA", "ú")
            .replace("\u00C3\u00BC", "ü")
            // Special symbols
            .replace("Âº", "º")
            .replace("Âª", "ª")
            .replace("Â¿", "¿")
            .replace("Â¡", "¡")
            .replace("â€“", "–")
            .replace("â€”", "—")
            .replace("â€œ", "“")
            .replace("â€", "”")
            .replace("â€˜", "‘")
            .replace("â€™", "’")
            .replace("Ã§", "ç")
            .replace("Ã‡", "Ç")

        // 2. Fix HTML and XML Entities
        text = text
            .replace("&Ntilde;", "Ñ")
            .replace("&ntilde;", "ñ")
            .replace("&#209;", "Ñ")
            .replace("&#241;", "ñ")
            .replace("&#x00D1;", "Ñ")
            .replace("&#x00F1;", "ñ")
            .replace("&Aacute;", "Á")
            .replace("&aacute;", "á")
            .replace("&Eacute;", "É")
            .replace("&eacute;", "é")
            .replace("&Iacute;", "Í")
            .replace("&iacute;", "í")
            .replace("&Oacute;", "Ó")
            .replace("&oacute;", "ó")
            .replace("&Uacute;", "Ú")
            .replace("&uacute;", "ú")
            .replace("&Uuml;", "Ü")
            .replace("&uuml;", "ü")
            .replace("&#193;", "Á")
            .replace("&#225;", "á")
            .replace("&#201;", "É")
            .replace("&#233;", "é")
            .replace("&#205;", "Í")
            .replace("&#237;", "í")
            .replace("&#211;", "Ó")
            .replace("&#243;", "ó")
            .replace("&#218;", "Ú")
            .replace("&#250;", "ú")
            .replace("&#220;", "Ü")
            .replace("&#252;", "ü")
            .replace("&deg;", "º")
            .replace("&#186;", "º")
            .replace("&#170;", "ª")
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")

        // 3. Fix URL percent-encodings if present
        text = text
            .replace("%C3%91", "Ñ")
            .replace("%C3%B1", "ñ")
            .replace("%C3%81", "Á")
            .replace("%C3%A1", "á")
            .replace("%C3%89", "É")
            .replace("%C3%A9", "é")
            .replace("%C3%8D", "Í")
            .replace("%C3%AD", "í")
            .replace("%C3%93", "Ó")
            .replace("%C3%B3", "ó")
            .replace("%C3%9A", "Ú")
            .replace("%C3%BA", "ú")

        // 4. Fix replacement characters (\uFFFD or ?) in common Spanish electoral surnames and words
        if (text.contains("\uFFFD")) {
            text = repairCommonSpanishWordsWithReplacementChar(text)
        }

        return text.trim()
    }

    private fun repairCommonSpanishWordsWithReplacementChar(input: String): String {
        // Specific surname & keyword patterns where \uFFFD occurs instead of Ñ or accents
        return input
            .replace("ACU\uFFFDA", "ACUÑA")
            .replace("acu\uFFFDa", "acuña")
            .replace("PE\uFFFDA", "PEÑA")
            .replace("pe\uFFFDa", "peña")
            .replace("IBA\uFFFD\uFFFDZ", "IBAÑEZ")
            .replace("IBA\uFFFD\uFFFD", "IBAÑEZ")
            .replace("IBA\uFFFDZ", "IBAÑEZ")
            .replace("iba\uFFFD\uFFFDz", "ibañez")
            .replace("NU\uFFFD\uFFFDZ", "NUÑEZ")
            .replace("NU\uFFFDZ", "NUÑEZ")
            .replace("nu\uFFFD\uFFFDz", "nuñez")
            .replace("MU\uFFFD\uFFFDZ", "MUÑOZ")
            .replace("MU\uFFFDZ", "MUÑOZ")
            .replace("mu\uFFFD\uFFFDz", "muñoz")
            .replace("CA\uFFFDETE", "CAÑETE")
            .replace("ca\uFFFDete", "cañete")
            .replace("CA\uFFFDIZA", "CAÑIZA")
            .replace("ZU\uFFFDIGA", "ZUÑIGA")
            .replace("zu\uFFFDiga", "zuñiga")
            .replace("ORDO\uFFFD\uFFFDZ", "ORDOÑEZ")
            .replace("ORDO\uFFFDZ", "ORDOÑEZ")
            .replace("ordo\uFFFD\uFFFDz", "ordoñez")
            .replace("BOLA\uFFFDOS", "BOLAÑOS")
            .replace("bola\uFFFDos", "bolaños")
            .replace("A\uFFFDA", "AÑA")
            .replace("A\uFFFDO", "AÑO")
            .replace("a\uFFFDa", "aña")
            .replace("a\uFFFDo", "año")
            .replace("ESPELTA", "ESPELTA")
            .replace("ESPA\uFFFDA", "ESPAÑA")
            .replace("espa\uFFFDA", "españa")
            .replace("N\uFFFD", "Nº")
            .replace("\uFFFD", "") // Clean remaining unresolvable replacement symbols
    }

    /**
     * Sanitizes all text fields of a [Voter] object to ensure clean display and persistence.
     */
    fun sanitizeVoter(voter: Voter): Voter {
        return voter.copy(
            fullName = sanitizeText(voter.fullName),
            cedula = sanitizeText(voter.cedula),
            votingPlace = sanitizeText(voter.votingPlace),
            tableNumber = sanitizeText(voter.tableNumber),
            orderNumber = sanitizeText(voter.orderNumber),
            address = sanitizeText(voter.address),
            cityOrZone = sanitizeText(voter.cityOrZone),
            phone = sanitizeText(voter.phone),
            notes = sanitizeText(voter.notes)
        )
    }

    /**
     * Sanitizes a list of [Voter] objects.
     */
    fun sanitizeVoters(voters: List<Voter>): List<Voter> {
        return voters.map { sanitizeVoter(it) }
    }
}
