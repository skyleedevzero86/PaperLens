package com.sleekydz86.paperlens.infrastructure.global.session

import com.sleekydz86.paperlens.application.dto.AuthResponse
import com.sleekydz86.paperlens.application.dto.SessionInfoResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.ObjectProvider
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.Session
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class RedisSessionStoreService(
    private val sessionRepositoryProvider: ObjectProvider<FindByIndexNameSessionRepository<Session>>,
    private val redisTemplateProvider: ObjectProvider<StringRedisTemplate>,
    @Value("\${app.session.tracking-enabled:false}")
    private val enabled: Boolean,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun trackAuthenticatedSession(request: HttpServletRequest, authResponse: AuthResponse) {
        if (!enabled || !isRedisSessionTrackingAvailable()) return
        val session = runCatching { request.getSession(true) }.getOrNull() ?: return
        applySessionAttributes(session, authResponse)
        runCatching {
            redisTemplate()?.opsForSet()?.add(ACTIVE_SESSIONS_KEY, session.id)
        }.onFailure {
            logger.warn("Failed to track authenticated session: sessionId={}", session.id, it)
        }
    }

    fun getCurrentSession(request: HttpServletRequest): SessionInfoResponse? {
        val httpSession = request.getSession(false) ?: return null
        return if (enabled) {
            findSession(httpSession.id) ?: basicSessionInfo(httpSession)
        } else {
            basicSessionInfo(httpSession)
        }
    }

    fun invalidateCurrentSession(request: HttpServletRequest) {
        val httpSession = request.getSession(false) ?: return
        runCatching {
            redisTemplate()?.opsForSet()?.remove(ACTIVE_SESSIONS_KEY, httpSession.id)
            httpSession.invalidate()
        }.onFailure {
            logger.warn("Failed to invalidate session: sessionId={}", httpSession.id, it)
        }
    }

    fun getActiveSessions(limit: Int): List<SessionInfoResponse> {
        if (!enabled) return emptyList()
        val ids = runCatching {
            redisTemplate()?.opsForSet()?.members(ACTIVE_SESSIONS_KEY).orEmpty()
        }.getOrElse {
            logger.warn("Failed to load active sessions from Redis", it)
            emptySet()
        }
        return ids.mapNotNull(::findSession)
            .sortedByDescending { it.lastAccessedAt }
            .take(limit.coerceIn(1, 500))
    }

    private fun findSession(sessionId: String): SessionInfoResponse? {
        val session = runCatching {
            sessionRepository()?.findById(sessionId)
        }.getOrElse {
            logger.warn("Failed to load session from repository: sessionId={}", sessionId, it)
            null
        }
            ?: run {
                runCatching {
                    redisTemplate()?.opsForSet()?.remove(ACTIVE_SESSIONS_KEY, sessionId)
                }.onFailure {
                    logger.warn("Failed to cleanup inactive session id: sessionId={}", sessionId, it)
                }
                return null
            }
        return SessionInfoResponse(
            sessionId = session.id,
            principal = session.getAttribute<String>(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME),
            displayName = session.getAttribute(ATTR_NAME),
            role = session.getAttribute(ATTR_ROLE),
            createdAt = session.creationTime.toString(),
            lastAccessedAt = session.lastAccessedTime.toString(),
            maxInactiveIntervalSeconds = session.maxInactiveInterval.seconds.toInt(),
        )
    }

    private fun basicSessionInfo(session: HttpSession): SessionInfoResponse =
        SessionInfoResponse(
            sessionId = session.id,
            principal = session.getAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME) as? String,
            displayName = session.getAttribute(ATTR_NAME) as? String,
            role = session.getAttribute(ATTR_ROLE) as? String,
            createdAt = Instant.ofEpochMilli(session.creationTime).toString(),
            lastAccessedAt = Instant.ofEpochMilli(session.lastAccessedTime).toString(),
            maxInactiveIntervalSeconds = session.maxInactiveInterval,
        )

    private fun applySessionAttributes(session: HttpSession, authResponse: AuthResponse) {
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, authResponse.email)
        session.setAttribute(ATTR_EMAIL, authResponse.email)
        session.setAttribute(ATTR_NAME, authResponse.name)
        session.setAttribute(ATTR_ROLE, authResponse.role)
        session.setAttribute(ATTR_LOGIN_AT, Instant.now().toString())
    }

    private fun sessionRepository(): FindByIndexNameSessionRepository<Session>? =
        sessionRepositoryProvider.ifAvailable

    private fun redisTemplate(): StringRedisTemplate? =
        redisTemplateProvider.ifAvailable

    private fun isRedisSessionTrackingAvailable(): Boolean {
        if (sessionRepository() == null || redisTemplate() == null) return false
        return runCatching {
            redisTemplate()!!.execute<String> { connection -> connection.ping() } != null
        }.getOrElse {
            logger.warn("Redis session tracking is disabled because Redis is unavailable", it)
            false
        }
    }

    companion object {
        private const val ACTIVE_SESSIONS_KEY = "paperlens:sessions:active"
        private const val ATTR_EMAIL = "paperlens.session.email"
        private const val ATTR_NAME = "paperlens.session.name"
        private const val ATTR_ROLE = "paperlens.session.role"
        private const val ATTR_LOGIN_AT = "paperlens.session.loginAt"
    }
}
