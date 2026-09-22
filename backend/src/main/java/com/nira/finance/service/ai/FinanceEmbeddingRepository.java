package com.nira.finance.service.ai;

import com.pgvector.PGvector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Raw JDBC access to the finance_embedding table. Spring Data JPA doesn't
 * have first-class support for the pgvector column type, so this is plain
 * JdbcTemplate + the pgvector-java helper instead of a JPA repository.
 */
@Repository
public class FinanceEmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;

    public FinanceEmbeddingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void deleteAllForUser(Long userId) {
        jdbcTemplate.update("DELETE FROM finance_embedding WHERE user_id = ?", userId);
    }

    public void insert(Long userId, String sourceType, Long sourceId, String content, float[] embedding) {
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO finance_embedding (user_id, source_type, source_id, content, embedding) " +
                            "VALUES (?, ?, ?, ?, ?)");
            ps.setLong(1, userId);
            ps.setString(2, sourceType);
            if (sourceId != null) ps.setLong(3, sourceId); else ps.setNull(3, java.sql.Types.BIGINT);
            ps.setString(4, content);
            ps.setObject(5, new PGvector(embedding));
            return ps;
        });
    }

    /** Cosine-distance nearest neighbors (pgvector's `<=>` operator) for this user. */
    public List<String> findSimilarContent(Long userId, float[] queryEmbedding, int topK) {
        String sql = "SELECT content FROM finance_embedding " +
                "WHERE user_id = ? ORDER BY embedding <=> ? LIMIT ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("content"),
                userId, new PGvector(queryEmbedding), topK);
    }
}
