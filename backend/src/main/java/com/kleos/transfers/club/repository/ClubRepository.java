package com.kleos.transfers.club.repository;

import com.kleos.transfers.club.entity.Club;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for club identity records.
 */
public interface ClubRepository extends JpaRepository<Club, UUID> {
}
