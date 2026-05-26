package com.tattoo.studio.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tattoo.studio.data.remote.dto.TattooWorkDetailDto
import com.tattoo.studio.data.remote.dto.TagDto
import com.tattoo.studio.data.remote.dto.UserPublicDto
import com.tattoo.studio.data.remote.dto.RoleDto

@Entity(tableName = "work_details")
data class WorkDetailEntity(
    @PrimaryKey val id: Int,
    val imageUrl: String,
    val description: String?,
    val status: String,
    val likeCount: Int,
    val rejectionReason: String?,
    val createdAt: String,
    val updatedAt: String,
    val isLiked: Boolean,
    val isFavorited: Boolean,
    val tagNames: String,
    // Master info
    val masterId: Int,
    val masterName: String,
    val masterAvatar: String?,
    val masterRoleName: String,
    val masterRoleLevel: Int
)

fun TattooWorkDetailDto.toEntity() = WorkDetailEntity(
    id = id,
    imageUrl = imageUrl,
    description = description,
    status = status,
    likeCount = likeCount,
    rejectionReason = rejectionReason,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isLiked = isLiked,
    isFavorited = isFavorited,
    tagNames = tags.joinToString(",") { it.name },
    masterId = master.id,
    masterName = master.fullName,
    masterAvatar = master.avatarUrl,
    masterRoleName = master.role.name,
    masterRoleLevel = master.role.level
)

fun WorkDetailEntity.toDto() = TattooWorkDetailDto(
    id = id,
    imageUrl = imageUrl,
    description = description,
    status = status,
    likeCount = likeCount,
    rejectionReason = rejectionReason,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isLiked = isLiked,
    isFavorited = isFavorited,
    tags = tagNames.split(",").filter { it.isNotEmpty() }.mapIndexed { index, name -> TagDto(index, name) },
    master = UserPublicDto(
        id = masterId,
        fullName = masterName,
        avatarUrl = masterAvatar,
        role = RoleDto(0, masterRoleName, masterRoleLevel)
    )
)
