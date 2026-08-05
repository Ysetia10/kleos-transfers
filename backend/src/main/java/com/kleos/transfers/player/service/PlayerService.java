package com.kleos.transfers.player.service;

import com.kleos.transfers.player.dto.CreatePlayerRequest;
import com.kleos.transfers.player.dto.PlayerResponse;
import com.kleos.transfers.player.dto.UpdatePlayerRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Defines player identity use cases.
 */
public interface PlayerService {

    PlayerResponse create(CreatePlayerRequest request);

    Page<PlayerResponse> findAll(Pageable pageable);

    PlayerResponse findById(UUID id);

    PlayerResponse update(UUID id, UpdatePlayerRequest request);

    void softDelete(UUID id);
}
