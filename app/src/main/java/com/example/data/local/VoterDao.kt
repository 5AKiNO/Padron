package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VoterDao {

    @Query("""
        SELECT * FROM voters 
        WHERE (
            :query = '' OR 
            REPLACE(REPLACE(cedula, '.', ''), ',', '') LIKE '%' || :query || '%' OR
            fullName LIKE '%' || :query || '%'
        )
        AND (:votingPlace = '' OR votingPlace = :votingPlace)
        AND (:tableNumber = '' OR tableNumber = :tableNumber)
        AND (:cityOrZone = '' OR cityOrZone = :cityOrZone)
        AND (
            :addressOrBarrio = '' OR 
            address LIKE '%' || :addressOrBarrio || '%' OR 
            cityOrZone LIKE '%' || :addressOrBarrio || '%'
        )
        AND (:votedFilter IS NULL OR voted = :votedFilter)
        AND (
            :hasPhoneFilter IS NULL OR 
            (:hasPhoneFilter = 1 AND phone IS NOT NULL AND phone != '') OR 
            (:hasPhoneFilter = 0 AND (phone IS NULL OR phone = ''))
        )
        AND (
            :notesKeyword = '' OR 
            notes LIKE '%' || :notesKeyword || '%'
        )
        ORDER BY fullName ASC
    """)
    fun searchAndFilterVoters(
        query: String,
        votingPlace: String = "",
        tableNumber: String = "",
        cityOrZone: String = "",
        addressOrBarrio: String = "",
        votedFilter: Boolean? = null,
        hasPhoneFilter: Boolean? = null,
        notesKeyword: String = ""
    ): Flow<List<Voter>>

    @Query("""
        SELECT * FROM voters 
        WHERE (:votingPlace = '' OR votingPlace = :votingPlace)
        AND (:tableNumber = '' OR tableNumber = :tableNumber)
        AND (:cityOrZone = '' OR cityOrZone = :cityOrZone)
        AND (
            :addressOrBarrio = '' OR 
            address LIKE '%' || :addressOrBarrio || '%' OR 
            cityOrZone LIKE '%' || :addressOrBarrio || '%'
        )
        AND (:votedFilter IS NULL OR voted = :votedFilter)
        AND (
            :hasPhoneFilter IS NULL OR 
            (:hasPhoneFilter = 1 AND phone IS NOT NULL AND phone != '') OR 
            (:hasPhoneFilter = 0 AND (phone IS NULL OR phone = ''))
        )
        AND (
            :notesKeyword = '' OR 
            notes LIKE '%' || :notesKeyword || '%'
        )
        ORDER BY fullName ASC
    """)
    fun getBaseFilteredVotersFlow(
        votingPlace: String = "",
        tableNumber: String = "",
        cityOrZone: String = "",
        addressOrBarrio: String = "",
        votedFilter: Boolean? = null,
        hasPhoneFilter: Boolean? = null,
        notesKeyword: String = ""
    ): Flow<List<Voter>>

    @Query("SELECT * FROM voters ORDER BY fullName ASC")
    fun getAllVotersFlow(): Flow<List<Voter>>

    @Query("SELECT * FROM voters ORDER BY fullName ASC")
    suspend fun getAllVotersList(): List<Voter>

    @Query("SELECT * FROM voters WHERE id = :id LIMIT 1")
    suspend fun getVoterById(id: Long): Voter?

    @Query("SELECT * FROM voters WHERE id IN (:ids) ORDER BY fullName ASC")
    suspend fun getVotersByIds(ids: List<Long>): List<Voter>

    @Query("UPDATE voters SET voted = :voted WHERE id IN (:ids)")
    suspend fun updateVotedStatusBulk(ids: List<Long>, voted: Boolean)

    @Query("SELECT * FROM voters WHERE cedula = :cedula LIMIT 1")
    suspend fun getVoterByCedula(cedula: String): Voter?

    @Query("SELECT COUNT(*) FROM voters")
    fun getVoterCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM voters")
    suspend fun getVoterCount(): Int

    @Query("SELECT COUNT(*) FROM voters WHERE voted = 1")
    fun getVotedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM voters WHERE phone IS NOT NULL AND TRIM(phone) != ''")
    fun getVotersWithPhoneCountFlow(): Flow<Int>

    @Query("SELECT * FROM voters WHERE phone IS NOT NULL AND TRIM(phone) != '' ORDER BY fullName ASC")
    suspend fun getVotersWithPhoneList(): List<Voter>

    @Query("SELECT DISTINCT votingPlace FROM voters WHERE votingPlace != '' ORDER BY votingPlace ASC")
    fun getVotingPlacesFlow(): Flow<List<String>>

    @Query("SELECT DISTINCT tableNumber FROM voters WHERE tableNumber != '' ORDER BY tableNumber ASC")
    fun getTableNumbersFlow(): Flow<List<String>>

    @Query("SELECT DISTINCT cityOrZone FROM voters WHERE cityOrZone != '' ORDER BY cityOrZone ASC")
    fun getCitiesOrZonesFlow(): Flow<List<String>>

    @Query("SELECT DISTINCT address FROM voters WHERE address != '' ORDER BY address ASC")
    fun getAddressesFlow(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoter(voter: Voter): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoters(voters: List<Voter>)

    @Update
    suspend fun updateVoter(voter: Voter)

    @Delete
    suspend fun deleteVoter(voter: Voter)

    @Query("DELETE FROM voters")
    suspend fun clearAll()
}
