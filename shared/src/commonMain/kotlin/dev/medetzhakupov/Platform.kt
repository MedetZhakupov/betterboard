package dev.medetzhakupov

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform