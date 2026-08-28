package com.example

import com.example.data.local.Voter
import com.example.util.FuzzySearchHelper
import com.example.util.TextEncodingSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextEncodingSanitizerTest {

    @Test
    fun testMojibakeAndEnieRepair() {
        // Test ACUÑA with mojibake
        assertEquals("ACUÑA", TextEncodingSanitizer.sanitizeText("ACUÃ‘A"))
        assertEquals("acuña", TextEncodingSanitizer.sanitizeText("acuÃ±a"))

        // Test HTML entities
        assertEquals("ACUÑA", TextEncodingSanitizer.sanitizeText("ACU&Ntilde;A"))
        assertEquals("ACUÑA", TextEncodingSanitizer.sanitizeText("ACU&#209;A"))
        assertEquals("acuña", TextEncodingSanitizer.sanitizeText("acu&ntilde;a"))

        // Test replacement character repair in known surnames
        assertEquals("ACUÑA", TextEncodingSanitizer.sanitizeText("ACU\uFFFDA"))
        assertEquals("PEÑA", TextEncodingSanitizer.sanitizeText("PE\uFFFDA"))
        assertEquals("NUÑEZ", TextEncodingSanitizer.sanitizeText("NU\uFFFDZ"))

        // Test accented vowels
        assertEquals("GONZÁLEZ", TextEncodingSanitizer.sanitizeText("GONZÃLEZ"))
        assertEquals("BENÍTEZ", TextEncodingSanitizer.sanitizeText("BENÃTEZ"))
    }

    @Test
    fun testFuzzySearchWithEnieAndN() {
        val voters = listOf(
            Voter(
                id = 1L,
                cedula = "1.234.567",
                fullName = "ACUÑA BENÍTEZ, RAMÓN",
                votingPlace = "Colegio Nacional",
                tableNumber = "1",
                orderNumber = "1"
            ),
            Voter(
                id = 2L,
                cedula = "7.654.321",
                fullName = "NUÑEZ PEÑA, LAURA",
                votingPlace = "Escuela República",
                tableNumber = "2",
                orderNumber = "2"
            )
        )

        // Typing "acuna" with plain 'n' must match "ACUÑA"
        val results1 = FuzzySearchHelper.filterAndRankByName(voters, "acuna")
        assertEquals(1, results1.size)
        assertEquals("ACUÑA BENÍTEZ, RAMÓN", results1[0].fullName)

        // Typing "acuña" with 'ñ' must also match "ACUÑA"
        val results2 = FuzzySearchHelper.filterAndRankByName(voters, "acuña")
        assertEquals(1, results2.size)
        assertEquals("ACUÑA BENÍTEZ, RAMÓN", results2[0].fullName)

        // Typing "nunez" must match "NUÑEZ PEÑA"
        val results3 = FuzzySearchHelper.filterAndRankByName(voters, "nunez")
        assertEquals(1, results3.size)
        assertEquals("NUÑEZ PEÑA, LAURA", results3[0].fullName)
    }
}
