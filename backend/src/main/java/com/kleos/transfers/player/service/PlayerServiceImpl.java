package com.kleos.transfers.player.service;

import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.common.bulk.BulkImportSpec;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.bulk.NaturalKeys;
import com.kleos.transfers.common.exception.ConflictException;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.player.dto.CreatePlayerRequest;
import com.kleos.transfers.player.dto.PlayerResponse;
import com.kleos.transfers.player.dto.UpdatePlayerRequest;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.player.mapper.PlayerMapper;
import com.kleos.transfers.player.repository.PlayerRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final BulkImporter bulkImporter;

    @Override
    @Transactional
    public PlayerResponse create(CreatePlayerRequest request) {
        assertUnique(request.fullName(), request.dateOfBirth(), request.nationality(), request.fbrefId(), null);
        Player player = playerMapper.toEntity(request);
        return playerMapper.toResponse(playerRepository.save(player));
    }

    @Override
    @Transactional
    public BulkImportResponse<PlayerResponse> createAll(List<CreatePlayerRequest> requests) {
        return bulkImporter.importAll(requests, new PlayerBulkSpec());
    }

    @Override
    public Page<PlayerResponse> findAll(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return playerRepository.findAll(pageable).map(playerMapper::toResponse);
        }
        // Native search owns ORDER BY; drop Sort so Spring does not append invalid property names.
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return playerRepository.searchByName(query.trim(), page).map(playerMapper::toResponse);
    }

    @Override
    public PlayerResponse findById(UUID id) {
        return playerMapper.toResponse(findPlayer(id));
    }

    @Override
    @Transactional
    public PlayerResponse update(UUID id, UpdatePlayerRequest request) {
        Player player = findPlayer(id);
        assertUnique(request.fullName(), request.dateOfBirth(), request.nationality(), request.fbrefId(), id);
        playerMapper.updateEntity(player, request);
        return playerMapper.toResponse(player);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        findPlayer(id).softDelete();
    }

    private Player findPlayer(UUID id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Player", id));
    }

    private void assertUnique(
            String fullName,
            java.time.LocalDate dateOfBirth,
            String nationality,
            String fbrefId,
            UUID excludingId
    ) {
        String normalizedName = fullName.trim().toLowerCase(Locale.ROOT);
        String normalizedNationality = nationality.trim().toUpperCase(Locale.ROOT);
        boolean nameTaken = excludingId == null
                ? playerRepository.existsByFullNameNormalizedAndDateOfBirthAndNationality(
                        normalizedName, dateOfBirth, normalizedNationality)
                : playerRepository.existsByFullNameNormalizedAndDateOfBirthAndNationalityAndIdNot(
                        normalizedName, dateOfBirth, normalizedNationality, excludingId);
        if (nameTaken) {
            throw new ConflictException(
                    "Player already exists for name/dateOfBirth/nationality: "
                            + fullName + " / " + dateOfBirth + " / " + normalizedNationality
            );
        }

        if (fbrefId == null || fbrefId.isBlank()) {
            return;
        }
        String normalizedFbref = fbrefId.trim();
        boolean fbrefTaken = excludingId == null
                ? playerRepository.existsByFbrefId(normalizedFbref)
                : playerRepository.existsByFbrefIdAndIdNot(normalizedFbref, excludingId);
        if (fbrefTaken) {
            throw new ConflictException("Player already exists for fbrefId: " + normalizedFbref);
        }
    }

    /**
     * Prefer FBref id when present; otherwise name + DOB + nationality.
     */
    private final class PlayerBulkSpec implements BulkImportSpec<CreatePlayerRequest, PlayerResponse> {

        @Override
        public String naturalKey(CreatePlayerRequest request) {
            if (request.fbrefId() != null && !request.fbrefId().isBlank()) {
                return NaturalKeys.of("fbref", request.fbrefId());
            }
            return NaturalKeys.of(request.fullName(), request.dateOfBirth(), request.nationality());
        }

        @Override
        public String reference(CreatePlayerRequest request) {
            return String.valueOf(request.fullName());
        }

        @Override
        public Set<String> findExistingKeys(List<CreatePlayerRequest> requests) {
            Set<String> existing = new java.util.HashSet<>();

            Set<String> fbrefIds = requests.stream()
                    .map(CreatePlayerRequest::fbrefId)
                    .filter(id -> id != null && !id.isBlank())
                    .map(String::trim)
                    .collect(Collectors.toSet());
            if (!fbrefIds.isEmpty()) {
                playerRepository.findAllByFbrefIdIn(fbrefIds).stream()
                        .map(player -> NaturalKeys.of("fbref", player.getFbrefId()))
                        .forEach(existing::add);
            }

            Set<String> names = requests.stream()
                    .map(request -> request.fullName().trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            playerRepository.findAllByNormalizedName(names).stream()
                    .map(player -> player.getFbrefId() != null
                            ? NaturalKeys.of("fbref", player.getFbrefId())
                            : NaturalKeys.of(
                                    player.getFullName(),
                                    player.getDateOfBirth(),
                                    player.getNationality()))
                    .forEach(existing::add);

            return existing;
        }

        @Override
        public List<PlayerResponse> persist(List<CreatePlayerRequest> accepted) {
            List<Player> players = accepted.stream().map(playerMapper::toEntity).toList();
            return playerRepository.saveAll(players).stream().map(playerMapper::toResponse).toList();
        }
    }
}
