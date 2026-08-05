package com.kleos.transfers.club.service;

import com.kleos.transfers.club.dto.ClubResponse;
import com.kleos.transfers.club.dto.CreateClubRequest;
import com.kleos.transfers.club.dto.UpdateClubRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Defines club identity use cases.
 */
public interface ClubService {

    ClubResponse create(CreateClubRequest request);

    Page<ClubResponse> findAll(Pageable pageable);

    ClubResponse findById(UUID id);

    ClubResponse update(UUID id, UpdateClubRequest request);

    void softDelete(UUID id);
}
