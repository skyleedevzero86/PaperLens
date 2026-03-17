package com.sleekydz86.paperlens.application.exception

class DuplicateEmailException(message: String = "이미 등록된 이메일입니다.") : RuntimeException(message)
