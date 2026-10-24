package com.trustvault.repo;

import com.trustvault.model.DocumentPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, String> {

    Optional<DocumentPermission> findByDocumentIdAndUserId(
            String documentId,
            String userId
    );

    List<DocumentPermission> findByDocumentId(String documentId);

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM DocumentPermission p
        WHERE p.document.id = :documentId
          AND p.user.id = :userId
    """)
    void deleteByDocumentIdAndUserId(
            @Param("documentId") String documentId,
            @Param("userId") String userId
    );
}