package com.example.data.model

data class VoterGroup(
    val id: String,
    val name: String,
    val createdAt: Long,
    val voterIds: List<Long>
)
