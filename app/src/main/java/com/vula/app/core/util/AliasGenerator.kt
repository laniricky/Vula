package com.vula.app.core.util

import java.util.Random

object AliasGenerator {
    private val adjectives = listOf(
        "Neon", "Cyber", "Cosmic", "Lunar", "Solar", "Electric", "Phantom", 
        "Shadow", "Crimson", "Azure", "Golden", "Silver", "Crystal", "Midnight",
        "Velvet", "Rusty", "Iron", "Silent", "Wandering", "Swift"
    )

    private val nouns = listOf(
        "Tiger", "Wolf", "Hawk", "Eagle", "Fox", "Bear", "Dragon", "Phoenix",
        "Falcon", "Raven", "Panther", "Lion", "Leopard", "Shark", "Viper",
        "Ghost", "Nomad", "Rider", "Pilot", "Walker"
    )

    fun generate(): String {
        val random = Random()
        val adj = adjectives[random.nextInt(adjectives.size)]
        val noun = nouns[random.nextInt(nouns.size)]
        val num = random.nextInt(90) + 10 // 10 to 99
        return "$adj$noun$num"
    }
}
