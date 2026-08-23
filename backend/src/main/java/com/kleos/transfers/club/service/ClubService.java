package com.kleos.transfers.club.service;

import com.kleos.transfers.club.dto.ClubResponse;
import com.kleos.transfers.club.dto.CreateClubRequest;
import com.kleos.transfers.club.dto.LikelyLineupResponse;
import com.kleos.transfers.club.dto.UpdateClubRequest;
import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.common.dto.UpdateIdentityMediaRequest;
import com.kleos.transfers.playerseason.dto.PlayerSeasonResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Defines club identity use cases.
 */
public interface ClubService {

    ClubResponse create(CreateClubRequest request);

    BulkImportResponse<ClubResponse> createAll(List<CreateClubRequest> requests);

    Page<ClubResponse> findAll(String query, Pageable pageable);

    ClubResponse findById(UUID id);

    ClubResponse update(UUID id, UpdateClubRequest request);

    ClubResponse updateMedia(UUID id, UpdateIdentityMediaRequest request);

    void softDelete(UUID id);

    /** Player-seasons for a club in one season (squad context for simulator / prediction). */
    List<PlayerSeasonResponse> findSquad(UUID clubId, UUID seasonId);

    /** Inferred starting XI from squad minutes and enriched positions. */
    LikelyLineupResponse findLikelyLineup(UUID clubId, UUID seasonId);
}
