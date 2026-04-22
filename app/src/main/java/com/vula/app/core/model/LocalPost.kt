package com.vula.app.core.model

data class LocalPost(
    val id: String = "",
    val networkId: String = "",
    val alias: String = "",
    val deviceIdHash: String = "",
    val text: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val reactionsCount: Int = 0
)
