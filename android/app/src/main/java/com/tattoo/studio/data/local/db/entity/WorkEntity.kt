package com.tattoo.studio.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "works")
data class WorkEntity(
    @PrimaryKey val id: Int,
    val imageUrl: String,
    val status: String,
    val likeCount: Int,
    val createdAt: String,
    // Store simple list of tags as comma separated string for caching feed
    val tagNames: String,
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val masterName: String = "МАСТЕР",
    val masterAvatar: String? = null,
    val masterId: Int = 0
)
