package com.sleekydz86.paperlens.application.port

interface TokenPort {

    fun generateToken(username: String, roles: List<String>): String
}