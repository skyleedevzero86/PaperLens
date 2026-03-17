package com.sleekydz86.paperlens.infrastructure.global.adapter

import com.sleekydz86.paperlens.application.port.QueryLogPort
import com.sleekydz86.paperlens.application.port.QueryLogStats
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class QueryLogAdapter(
    private val entityManager: EntityManager,
) : QueryLogPort {

    @Transactional
    override fun log(userId: Long, documentId: Long, question: String, answer: String, latencyMs: Long, modelName: String) {
        entityManager.createNativeQuery("""
            INSERT INTO ai_query_logs (user_id, document_id, question, answer, latency_ms, model_name, created_at)
            VALUES (?, ?, ?, ?, ?, ?, NOW())
        """)
            .setParameter(1, userId)
            .setParameter(2, documentId)
            .setParameter(3, question)
            .setParameter(4, answer)
            .setParameter(5, latencyMs)
            .setParameter(6, modelName)
            .executeUpdate()
    }

    override fun getStats(): QueryLogStats {
        val totalQueries = (entityManager.createNativeQuery("SELECT COUNT(*) FROM ai_query_logs")
            .singleResult as Number).toLong()
        val avgLatency = (entityManager.createNativeQuery("SELECT COALESCE(AVG(latency_ms), 0) FROM ai_query_logs")
            .singleResult as Number).toDouble()
        return QueryLogStats(totalQueries = totalQueries, avgLatencyMs = avgLatency)
    }
}
