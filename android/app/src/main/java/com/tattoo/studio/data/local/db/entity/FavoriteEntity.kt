package com.tattoo.studio.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tattoo.studio.data.remote.dto.TattooWorkFeedDto
import com.tattoo.studio.data.remote.dto.TagDto

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: Int,
    val imageUrl: String,
    val status: String,
    val likeCount: Int,
    val createdAt: String,
    val tagNames: String,
    val isLiked: Boolean,
    val isFavorited: Boolean,
    val masterName: String,
    val masterAvatar: String?,
    val masterId: Int
)

fun TattooWorkFeedDto.toFavoriteEntity() = FavoriteEntity(
    id = id,
    imageUrl = imageUrl,
    status = status,
    likeCount = likeCount,
    createdAt = createdAt,
    tagNames = tags.joinToString(",") { it.name },
    isLiked = isLiked,
    isFavorited = isFavorited,
    masterName = masterName,
    masterAvatar = masterAvatar,
    masterId = masterId
)

fun FavoriteEntity.toDto() = TattooWorkFeedDto(
    id = id,
    imageUrl = imageUrl,
    status = status,
    likeCount = likeCount,
    createdAt = createdAt,
    tags = tagNames.split(",").filter { it.isNotEmpty() }.mapIndexed { index, name -> TagDto(index, name) },
    isLiked = isLiked,
    isFavorited = isFavorited,
    masterName = masterName,
    masterAvatar = masterAvatar,
    masterId = masterId
)
