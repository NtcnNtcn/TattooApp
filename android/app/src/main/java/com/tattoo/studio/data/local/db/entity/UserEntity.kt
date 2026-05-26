package com.tattoo.studio.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tattoo.studio.data.remote.dto.UserDto
import com.tattoo.studio.data.remote.dto.RoleDto
import com.tattoo.studio.data.remote.dto.UserStatsDto

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val id: Int,
    val email: String,
    val fullName: String,
    val avatarUrl: String?,
    val roleName: String,
    val roleLevel: Int,
    // Stats
    val likesGiven: Int,
    val likesReceived: Int,
    val favoritesCount: Int,
    val worksCount: Int,
    val followingCount: Int,
    val followersCount: Int,
    val applicationsCount: Int,
    val favoritesReceived: Int,
    val description: String?,
    val status: String?,
    val isVerified: Boolean,
    val createdAt: String
)

fun UserDto.toEntity() = UserEntity(
    id = id,
    email = email,
    fullName = fullName,
    avatarUrl = avatarUrl,
    roleName = role.name,
    roleLevel = role.level,
    likesGiven = stats?.likesGiven ?: 0,
    likesReceived = stats?.likesReceived ?: 0,
    favoritesCount = stats?.favoritesCount ?: 0,
    worksCount = stats?.worksCount ?: 0,
    followingCount = stats?.followingCount ?: 0,
    followersCount = stats?.followersCount ?: 0,
    applicationsCount = stats?.applicationsCount ?: 0,
    favoritesReceived = stats?.favoritesReceived ?: 0,
    description = description,
    status = status,
    isVerified = isVerified,
    createdAt = createdAt
)

fun UserEntity.toDto() = UserDto(
    id = id,
    email = email,
    fullName = fullName,
    avatarUrl = avatarUrl,
    isVerified = isVerified,
    role = RoleDto(0, roleName, roleLevel),
    stats = UserStatsDto(
        likesGiven = likesGiven,
        likesReceived = likesReceived,
        favoritesCount = favoritesCount,
        worksCount = worksCount,
        followingCount = followingCount,
        followersCount = followersCount,
        applicationsCount = applicationsCount,
        favoritesReceived = favoritesReceived
    ),
    description = description,
    status = status,
    createdAt = createdAt
)
