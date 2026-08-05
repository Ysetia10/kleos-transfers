package com.kleos.transfers.repository;

import com.kleos.transfers.entity.Player;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for player identity records.
 */
public interface PlayerRepository extends JpaRepository<Player, UUID> {
}
