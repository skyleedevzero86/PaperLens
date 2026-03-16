package com.sleekydz86.paperlens.application.port

import com.sleekydz86.paperlens.domain.user.User

interface AuthPort {

    fun authenticate(email: String, password: String): User?
}
