package com.sleekydz86.paperlens.infrastructure.global.adapter

import com.sleekydz86.paperlens.application.port.PasswordEncoderPort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class PasswordEncoderAdapter(
    private val delegate: PasswordEncoder,
) : PasswordEncoderPort {

    override fun encode(rawPassword: String): String =
        delegate.encode(rawPassword) ?: error("비밀번호 인코더가 null을 반환했습니다.")
}
