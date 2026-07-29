package com.sultan.kaspitracker.repository;

import com.sultan.kaspitracker.entity.Statement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatementRepository extends JpaRepository<Statement, Long> {
    
    /**
     * Finds a statement by its unique SHA-256 file hash.
     * Used for duplicate upload protection.
     */
    Optional<Statement> findByFileHash(String fileHash);
}
