package de.tabmates.features.tabgroup.database.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity
data class GroupEntity(
    @PrimaryKey
    val groupId: String,
    val title: String,
    val description: String,
    val defaultCurrencyCode: String,
    val creatorId: String,
    val lastModifiedAt: Long,
)
