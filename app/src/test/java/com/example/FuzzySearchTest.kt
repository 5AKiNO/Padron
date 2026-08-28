package com.example

import com.example.data.local.Voter
import com.example.util.FuzzySearchHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzySearchTest {

    private val sampleVoters = listOf(
        Voter(
            id = 1,
            cedula = "1.234.567",
            fullName = "GÓMEZ BENÍTEZ, JUAN CARLOS",
            votingPlace = "Colegio Nacional",
            tableNumber = "01",
            orderNumber = "1"
        ),
        Voter(
            id = 2,
            cedula = "2.345.678",
            fullName = "GONZÁLEZ PÉREZ, MARÍA JOSÉ",
            votingPlace = "Escuela Graduada",
            tableNumber = "02",
            orderNumber = "2"
        ),
        Voter(
            id = 3,
            cedula = "3.456.789",
            fullName = "RODRÍGUEZ ACOSTA, ESTEBAN",
            votingPlace = "Colegio Nacional",
            tableNumber = "01",
            orderNumber = "3"
        ),
        Voter(
            id = 4,
            cedula = "4.567.890",
            fullName = "ÁLVAREZ CABAÑAS, ANA LAURA",
            votingPlace = "Colegio Nacional",
            tableNumber = "03",
            orderNumber = "4"
        )
    )

    @Test
    fun testAccentInsensitiveSearch() {
        val results = FuzzySearchHelper.filterAndRankByName(sampleVoters, "gomez")
        assertEquals(1, results.size)
        assertEquals("GÓMEZ BENÍTEZ, JUAN CARLOS", results.first().fullName)

        val resultsAlvarez = FuzzySearchHelper.filterAndRankByName(sampleVoters, "alvarez")
        assertEquals(1, resultsAlvarez.size)
        assertEquals("ÁLVAREZ CABAÑAS, ANA LAURA", resultsAlvarez.first().fullName)
    }

    @Test
    fun testMultiTokenOrderIndependentSearch() {
        // Query in reverse order from the recorded name
        val results = FuzzySearchHelper.filterAndRankByName(sampleVoters, "Juan Gomez")
        assertEquals(1, results.size)
        assertEquals("GÓMEZ BENÍTEZ, JUAN CARLOS", results.first().fullName)

        val resultsMaria = FuzzySearchHelper.filterAndRankByName(sampleVoters, "Jose Gonzalez")
        assertEquals(1, resultsMaria.size)
        assertEquals("GONZÁLEZ PÉREZ, MARÍA JOSÉ", resultsMaria.first().fullName)
    }

    @Test
    fun testApproximateTypoFuzzySearch() {
        // "Gonzales" with 's' should match "GONZÁLEZ" with 'z'
        val results = FuzzySearchHelper.filterAndRankByName(sampleVoters, "Gonzales")
        assertTrue(results.isNotEmpty())
        assertEquals("GONZÁLEZ PÉREZ, MARÍA JOSÉ", results.first().fullName)

        // "Estevan" with 'v' should match "ESTEBAN" with 'b'
        val resultsEsteban = FuzzySearchHelper.filterAndRankByName(sampleVoters, "Estevan")
        assertTrue(resultsEsteban.isNotEmpty())
        assertEquals("RODRÍGUEZ ACOSTA, ESTEBAN", resultsEsteban.first().fullName)
    }

    @Test
    fun testCedulaSearch() {
        // Searching clean digits
        val results1 = FuzzySearchHelper.filterByCedula(sampleVoters, "1234567")
        assertEquals(1, results1.size)
        assertEquals("1.234.567", results1.first().cedula)

        // Searching with formatted input (prefix match takes priority first)
        val results2 = FuzzySearchHelper.filterByCedula(sampleVoters, "2.345")
        assertTrue(results2.isNotEmpty())
        assertEquals("2.345.678", results2.first().cedula)
    }
}
