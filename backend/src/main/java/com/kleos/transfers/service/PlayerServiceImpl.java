package com.kleos.transfers.service;

import com.kleos.transfers.dto.CreatePlayerRequest;
import com.kleos.transfers.dto.PlayerResponse;
import com.kleos.transfers.dto.UpdatePlayerRequest;
import com.kleos.transfers.entity.Player;
import com.kleos.transfers.exception.ResourceNotFoundException;
import com.kleos.transfers.mapper.PlayerMapper;
import com.kleos.transfers.repository.PlayerRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for player identity use cases.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;

    @Override
    @Transactional
    public PlayerResponse create(CreatePlayerRequest request) {
        Player player = playerMapper.toEntity(request);
        return playerMapper.toResponse(playerRepository.save(player));
    }

    @Override
    public Page<PlayerResponse> findAll(Pageable pageable) {
        return playerRepository.findAll(pageable).map(playerMapper::toResponse);
    }

    @Override
    public PlayerResponse findById(UUID id) {
        return playerMapper.toResponse(findPlayer(id));
    }

    @Override
    @Transactional
    public PlayerResponse update(UUID id, UpdatePlayerRequest request) {
        Player player = findPlayer(id);
        playerMapper.updateEntity(player, request);
        return playerMapper.toResponse(player);
    }

    private Player findPlayer(UUID id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
    }
}
