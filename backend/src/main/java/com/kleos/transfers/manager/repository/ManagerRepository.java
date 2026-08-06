package com.kleos.transfers.manager.repository;

import com.kleos.transfers.manager.entity.Manager;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for manager identity records.
 */
public interface ManagerRepository extends JpaRepository<Manager, UUID> {
}
