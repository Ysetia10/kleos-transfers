package com.kleos.transfers.tournament.service;

import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.tournament.dto.CreateTournamentRequest;
import com.kleos.transfers.tournament.dto.TournamentResponse;
import com.kleos.transfers.tournament.dto.UpdateTournamentRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Defines tournament identity use cases.
 */
public interface TournamentService {

    TournamentResponse create(CreateTournamentRequest request);

    BulkImportResponse<TournamentResponse> createAll(List<CreateTournamentRequest> requests);

    Page<TournamentResponse> findAll(Pageable pageable);

    TournamentResponse findById(UUID id);

    TournamentResponse update(UUID id, UpdateTournamentRequest request);

    void softDelete(UUID id);
}
