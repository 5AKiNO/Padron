package com.example.util

import com.example.data.local.Voter
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

enum class SearchInputMode(val title: String, val hint: String) {
    CEDULA("Nº de Cédula", "Ingrese número de cédula (teclado numérico)..."),
    NOMBRE_APELLIDO("Nombre y Apellido", "Buscar por Apellido o Nombre (coincidencia rápida y aproximada)...")
}

data class CachedNameTokens(
    val normalizedFullName: String,
    val tokens: List<String>,
    val phoneticTokens: List<String>
)

object FuzzySearchHelper {

    // Fast in-memory cache for voter name tokens to eliminate redundant string processing
    private val nameTokensCache = ConcurrentHashMap<String, CachedNameTokens>(2048)

    /**
     * Ultra-fast character-by-character normalizer:
     * - Converts uppercase to lowercase.
     * - Handles NFC and NFD decomposed Unicode (e.g. n + combining tilde \u0303 -> 'ñ').
     * - Maps accented characters (á, é, í, ó, ú, ü, etc.) to standard ASCII.
     * - Preserves 'ñ'.
     * - Replaces punctuation with spaces without regex allocations.
     */
    fun fastNormalize(input: String): String {
        if (input.isEmpty()) return ""
        val len = input.length
        val sb = StringBuilder(len)
        var lastWasSpace = false

        for (i in 0 until len) {
            val c = input[i]
            
            // Handle combining marks from decomposed Unicode (NFD)
            if (c == '\u0303') { // Combining tilde
                if (sb.isNotEmpty() && sb[sb.length - 1] == 'n') {
                    sb.setCharAt(sb.length - 1, 'ñ')
                }
                continue
            }
            if (c == '\u0300' || c == '\u0301' || c == '\u0302' || c == '\u0308' || c == '\u030a' || c == '\u0327') {
                // Ignore combining acute, grave, diaeresis, etc.
                continue
            }

            val normChar: Char = when (c) {
                'á', 'Á', 'à', 'À', 'ä', 'Ä', 'â', 'Â', 'ã', 'Ã' -> 'a'
                'é', 'É', 'è', 'È', 'ë', 'Ë', 'ê', 'Ê' -> 'e'
                'í', 'Í', 'ì', 'Ì', 'ï', 'Ï', 'î', 'Î' -> 'i'
                'ó', 'Ó', 'ò', 'Ò', 'ö', 'Ö', 'ô', 'Ô', 'õ', 'Õ' -> 'o'
                'ú', 'Ú', 'ù', 'Ù', 'ü', 'Ü', 'û', 'Û' -> 'u'
                'ñ', 'Ñ' -> 'ñ'
                in 'a'..'z', in '0'..'9' -> c
                in 'A'..'Z' -> (c.code + 32).toChar()
                ' ', '\t', '\n', '\r', ',', '.', '-', '_', '/', '\\', '(', ')', '[', ']', '{', '}', '"', '\'', ';', ':', 'º', 'ª' -> ' '
                else -> ' '
            }

            if (normChar == ' ') {
                if (!lastWasSpace && sb.isNotEmpty()) {
                    sb.append(' ')
                    lastWasSpace = true
                }
            } else {
                sb.append(normChar)
                lastWasSpace = false
            }
        }

        return if (sb.isNotEmpty() && sb[sb.length - 1] == ' ') {
            sb.substring(0, sb.length - 1)
        } else {
            sb.toString()
        }
    }

    /**
     * Backwards-compatible normalize alias.
     */
    fun normalize(input: String): String = fastNormalize(input)

    /**
     * Fast phonetic key generator for Spanish names:
     * (b <-> v, s <-> z, c(e/i) <-> s, y <-> i, silent h, ñ <-> n, etc.)
     */
    fun spanishPhoneticKey(input: String): String {
        val norm = fastNormalize(input)
        if (norm.isEmpty()) return ""
        val sb = StringBuilder(norm.length)
        var i = 0
        while (i < norm.length) {
            val c = norm[i]
            val next = if (i + 1 < norm.length) norm[i + 1] else ' '
            when {
                c == 'h' -> {
                    // skip silent h
                }
                c == 'ñ' -> sb.append('n')
                c == 'v' -> sb.append('b')
                c == 'z' -> sb.append('s')
                c == 'c' && (next == 'e' || next == 'i') -> sb.append('s')
                c == 'y' -> sb.append('i')
                c == 'l' && next == 'l' -> {
                    sb.append('i')
                    i++
                }
                c == 'g' && (next == 'e' || next == 'i') -> sb.append('j')
                c == 'x' -> sb.append('s')
                else -> sb.append(c)
            }
            i++
        }
        return sb.toString()
    }

    /**
     * Fast Levenshtein distance with early-exit optimization when distance exceeds maxAllowed.
     */
    fun levenshteinDistance(s1: String, s2: String, maxAllowed: Int = 2): Int {
        val l1 = s1.length
        val l2 = s2.length
        if (kotlin.math.abs(l1 - l2) > maxAllowed) return maxAllowed + 1
        if (l1 == 0) return l2
        if (l2 == 0) return l1

        var prev = IntArray(l2 + 1) { it }
        var curr = IntArray(l2 + 1)

        for (i in 1..l1) {
            curr[0] = i
            var minInRow = curr[0]
            val c1 = s1[i - 1]
            for (j in 1..l2) {
                val cost = if (c1 == s2[j - 1]) 0 else 1
                curr[j] = min(
                    min(curr[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost
                )
                if (curr[j] < minInRow) minInRow = curr[j]
            }
            if (minInRow > maxAllowed) return maxAllowed + 1
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[l2]
    }

    private fun getOrCreateTokens(fullName: String): CachedNameTokens {
        val cached = nameTokensCache[fullName]
        if (cached != null) return cached

        val norm = fastNormalize(fullName)
        val tokens = if (norm.isEmpty()) emptyList() else norm.split(" ").filter { it.isNotEmpty() }
        val phonetics = tokens.map { spanishPhoneticKey(it) }
        val created = CachedNameTokens(norm, tokens, phonetics)
        if (nameTokensCache.size < 5000) {
            nameTokensCache[fullName] = created
        }
        return created
    }

    /**
     * High-speed token score evaluator:
     * Evaluates exact, prefix, substring, phonetic, and typo match in order of computational cost.
     */
    fun scoreTokenMatch(
        qToken: String,
        qPhonetic: String,
        nameToken: String,
        namePhonetic: String
    ): Int {
        if (qToken == nameToken) return 100
        if (nameToken.startsWith(qToken)) return 95
        if (nameToken.contains(qToken)) return 85

        // Equivalent ñ <-> n match (e.g. nuñez <-> nunez, peña <-> pena)
        val qPlainN = if (qToken.contains('ñ')) qToken.replace('ñ', 'n') else qToken
        val nPlainN = if (nameToken.contains('ñ')) nameToken.replace('ñ', 'n') else nameToken
        if (qPlainN == nPlainN) return 98
        if (nPlainN.startsWith(qPlainN)) return 92
        if (nPlainN.contains(qPlainN)) return 82

        // Phonetic fast match
        if (qPhonetic.isNotEmpty() && namePhonetic.isNotEmpty()) {
            if (qPhonetic == namePhonetic) return 90
            if (namePhonetic.startsWith(qPhonetic)) return 85
            if (namePhonetic.contains(qPhonetic)) return 80
        }

        // Fuzzy Levenshtein only for tokens of length >= 3
        val qLen = qToken.length
        val nLen = nameToken.length
        if (qLen >= 3 && nLen >= 3) {
            val dist = levenshteinDistance(qToken, nameToken, maxAllowed = 1)
            if (dist <= 1) return 75

            if (nLen > qLen) {
                val prefix = nameToken.substring(0, qLen)
                if (levenshteinDistance(qToken, prefix, maxAllowed = 1) <= 1) return 70
            }
        }

        return 0
    }

    /**
     * Filters and sorts voters with near-zero latency by pre-compiling query tokens
     * and reusing token caches.
     */
    fun filterAndRankByName(voters: List<Voter>, query: String): List<Voter> {
        val normQuery = fastNormalize(query)
        if (normQuery.isEmpty()) return voters

        val queryTokens = normQuery.split(" ").filter { it.isNotEmpty() }
        if (queryTokens.isEmpty()) return voters

        val queryPhonetics = queryTokens.map { spanishPhoneticKey(it) }
        val isSingleToken = queryTokens.size == 1

        val scoredVoters = mutableListOf<Pair<Voter, Int>>()

        for (i in 0 until voters.size) {
            val voter = voters[i]
            val cachedName = getOrCreateTokens(voter.fullName)
            val normFullName = cachedName.normalizedFullName

            // Fast full-string match check (both exact and ñ <-> n equivalent)
            if (normFullName.contains(normQuery)) {
                val isPrefix = normFullName.startsWith(normQuery)
                scoredVoters.add(Pair(voter, if (isPrefix) 250 else 200))
                continue
            }

            val normFullNamePlainN = if (normFullName.contains('ñ')) normFullName.replace('ñ', 'n') else normFullName
            val normQueryPlainN = if (normQuery.contains('ñ')) normQuery.replace('ñ', 'n') else normQuery
            if (normFullNamePlainN.contains(normQueryPlainN)) {
                val isPrefix = normFullNamePlainN.startsWith(normQueryPlainN)
                scoredVoters.add(Pair(voter, if (isPrefix) 245 else 195))
                continue
            }

            // Check multi-token match
            var totalScore = 0
            var allMatched = true

            for (qIdx in 0 until queryTokens.size) {
                val qTok = queryTokens[qIdx]
                val qPhon = queryPhonetics[qIdx]
                var bestTokenScore = 0

                for (nIdx in 0 until cachedName.tokens.size) {
                    val nTok = cachedName.tokens[nIdx]
                    val nPhon = cachedName.phoneticTokens[nIdx]
                    val score = scoreTokenMatch(qTok, qPhon, nTok, nPhon)
                    if (score > bestTokenScore) {
                        bestTokenScore = score
                        if (score == 100) break // perfect match for this query token
                    }
                }

                if (bestTokenScore == 0) {
                    allMatched = false
                    break
                }
                totalScore += bestTokenScore
            }

            if (allMatched && totalScore > 0) {
                scoredVoters.add(Pair(voter, totalScore))
            }
        }

        return scoredVoters
            .sortedWith(
                compareByDescending<Pair<Voter, Int>> { it.second }
                    .thenBy { it.first.fullName }
            )
            .map { it.first }
    }

    /**
     * Filters voters by Cedula number (digits-only, lightning fast).
     */
    fun filterByCedula(voters: List<Voter>, query: String): List<Voter> {
        val cleanDigits = StringBuilder(query.length)
        for (i in 0 until query.length) {
            val c = query[i]
            if (c in '0'..'9') cleanDigits.append(c)
        }
        val cleanQuery = cleanDigits.toString()

        if (cleanQuery.isEmpty()) {
            val rawClean = query.trim()
            if (rawClean.isEmpty()) return voters
            return voters.filter { it.cedula.contains(rawClean, ignoreCase = true) }
        }

        val exactMatches = mutableListOf<Voter>()
        val prefixMatches = mutableListOf<Voter>()
        val containsMatches = mutableListOf<Voter>()

        for (i in 0 until voters.size) {
            val voter = voters[i]
            val rawCedula = voter.cedula
            // Fast inline digit extraction
            val voterDigits = StringBuilder(rawCedula.length)
            for (j in 0 until rawCedula.length) {
                val c = rawCedula[j]
                if (c in '0'..'9') voterDigits.append(c)
            }
            val normCedula = voterDigits.toString()

            if (normCedula == cleanQuery) {
                exactMatches.add(voter)
            } else if (normCedula.startsWith(cleanQuery)) {
                prefixMatches.add(voter)
            } else if (normCedula.contains(cleanQuery)) {
                containsMatches.add(voter)
            }
        }

        exactMatches.sortBy { it.fullName }
        prefixMatches.sortBy { it.fullName }
        containsMatches.sortBy { it.fullName }

        return exactMatches + prefixMatches + containsMatches
    }
}
