package com.sleekydz86.paperlens.infrastructure.persistence.entity

import com.sleekydz86.paperlens.domain.user.UserRole
import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class UserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(nullable = false)
    private var password: String,

    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    val role: UserRole = UserRole.USER,

    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    var deletedAt: LocalDateTime? = null,
) : UserDetails {


    protected constructor() : this(
        id = 0,
        email = "",
        password = "",
        name = "",
    )

    override fun getUsername(): String = email
    override fun getPassword(): String = password
    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = deletedAt == null

    fun updatePassword(encoded: String) {
        password = encoded
    }
}
