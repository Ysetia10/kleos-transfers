package com.kleos.transfers.service;

import com.kleos.transfers.dto.CreatePlayerRequest;
import com.kleos.transfers.dto.PlayerResponse;
import com.kleos.transfers.dto.UpdatePlayerRequest;
import java.util.List;
import java.util.UUID;

/**
 * Defines player identity use cases.
 */
public interface PlayerService {

    PlayerResponse create(CreatePlayerRequest request);

    List<PlayerResponse> findAll();

    PlayerResponse findById(UUID id);

    PlayerResponse update(UUID id, UpdatePlayerRequest request);
}
