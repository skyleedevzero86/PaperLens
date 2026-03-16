package com.sleekydz86.paperlens.application.port

interface PasswordEncoderPort {

    fun encode(rawPassword: String): String
}
