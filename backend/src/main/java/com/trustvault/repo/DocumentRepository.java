package com.trustvault.repo;

import com.trustvault.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, String> {

    @Query(value = """
        SELECT d.*
        FROM documents d
        JOIN app_users o
          ON o.id = d.owner_id
        WHERE d.deleted = false

          AND (
              :admin = true
              OR d.owner_id = :uid
              OR EXISTS (
                  SELECT 1
                  FROM document_permissions dp
                  WHERE dp.document_id = d.id
                    AND dp.user_id = :uid
              )
          )

          AND (
              :name IS NULL
              OR LOWER(d.name) LIKE LOWER('%' || :name || '%')
          )

          AND (
              :category IS NULL
              OR LOWER(d.category) LIKE LOWER('%' || :category || '%')
          )

          AND (
              :owner IS NULL
              OR LOWER(o.email) LIKE LOWER('%' || :owner || '%')
              OR LOWER(o.full_name) LIKE LOWER('%' || :owner || '%')
          )

          AND (
              CAST(:fromDate AS TIMESTAMP WITH TIME ZONE) IS NULL
              OR d.created_at >= CAST(:fromDate AS TIMESTAMP WITH TIME ZONE)
          )

          AND (
              CAST(:toDate AS TIMESTAMP WITH TIME ZONE) IS NULL
              OR d.created_at < CAST(:toDate AS TIMESTAMP WITH TIME ZONE)
          )

        ORDER BY d.created_at DESC
        """, nativeQuery = true)
    List<Document> search(
        @Param("uid") String uid,
        @Param("admin") boolean admin,
        @Param("name") String name,
        @Param("category") String category,
        @Param("owner") String owner,
        @Param("fromDate") Instant fromDate,
        @Param("toDate") Instant toDate
    );
}