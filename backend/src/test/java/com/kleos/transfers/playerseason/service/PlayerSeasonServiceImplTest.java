package com.kleos.transfers.playerseason.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.club.repository.ClubRepository;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.domain.DateOfBirthPrecision;
import com.kleos.transfers.domain.Position;
import com.kleos.transfers.domain.PreferredFoot;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.player.repository.PlayerRepository;
import com.kleos.transfers.playerseason.dto.CreatePlayerSeasonRequest;
import com.kleos.transfers.playerseason.dto.PlayerSeasonResponse;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import com.kleos.transfers.playerseason.mapper.PlayerSeasonMapper;
import com.kleos.transfers.playerseason.repository.PlayerSeasonRepository;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.season.repository.SeasonRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerSeasonServiceImplTest {

    @Mock
    private PlayerSeasonRepository playerSeasonRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private PlayerSeasonMapper playerSeasonMapper;

    @Mock
    private BulkImporter bulkImporter;

    @InjectMocks
    private PlayerSeasonServiceImpl playerSeasonService;

    @Test
    void createsPlayerSeasonFromResolvedIdentities() {
        UUID playerId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        UUID seasonId = UUID.randomUUID();
        CreatePlayerSeasonRequest request = createRequest(playerId, clubId, seasonId);

        Player player = player(playerId);
        Club club = club(clubId);
        Season season = season(seasonId);
        PlayerSeason entity = entity(player, club, season);
        PlayerSeasonResponse expected = response(playerId, clubId, seasonId);

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season));
        when(playerSeasonMapper.toEntity(player, club, season, request)).thenReturn(entity);
        when(playerSeasonRepository.save(entity)).thenReturn(entity);
        when(playerSeasonMapper.toResponse(entity)).thenReturn(expected);

        assertThat(playerSeasonService.create(request)).isSameAs(expected);
        verify(playerSeasonRepository).save(entity);
    }

    @Test
    void rejectsUnknownPlayer() {
        UUID playerId = UUID.randomUUID();
        when(playerRepository.findById(playerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerSeasonService.create(
                createRequest(playerId, UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(playerId.toString());
    }

    @Test
    void softDeletesExistingPlayerSeason() {
        UUID id = UUID.randomUUID();
        PlayerSeason playerSeason = entity(
                player(UUID.randomUUID()), club(UUID.randomUUID()), season(UUID.randomUUID()));
        setId(playerSeason, id);

        when(playerSeasonRepository.findById(id)).thenReturn(Optional.of(playerSeason));

        playerSeasonService.softDelete(id);

        assertThat(playerSeason.isDeleted()).isTrue();
        assertThat(playerSeason.getUniquenessKey()).endsWith("#" + id);
    }

    private CreatePlayerSeasonRequest createRequest(UUID playerId, UUID clubId, UUID seasonId) {
        return new CreatePlayerSeasonRequest(
                playerId,
                clubId,
                seasonId,
                30,
                2450,
                8,
                5,
                new BigDecimal("7.20"),
                new BigDecimal("4.10"),
                Position.RW
        );
    }

    private PlayerSeason entity(Player player, Club club, Season season) {
        return new PlayerSeason(
                player,
                club,
                season,
                30,
                2450,
                8,
                5,
                new BigDecimal("7.20"),
                new BigDecimal("4.10"),
                Position.RW
        );
    }

    private Player player(UUID id) {
        Player player = new Player(
                "Bukayo Saka",
                LocalDate.of(2001, 9, 5),
                DateOfBirthPrecision.DAY,
                "ENG",
                178,
                PreferredFoot.LEFT,
                Position.RW
        );
        setId(player, id);
        return player;
    }

    private Club club(UUID id) {
        Club club = new Club("Arsenal", "ARS", "ENG", 1886);
        setId(club, id);
        return club;
    }

    private Season season(UUID id) {
        Season season = new Season("2024/25", LocalDate.of(2024, 7, 1), LocalDate.of(2025, 6, 30));
        setId(season, id);
        return season;
    }

    private PlayerSeasonResponse response(UUID playerId, UUID clubId, UUID seasonId) {
        return new PlayerSeasonResponse(
                UUID.randomUUID(),
                playerId,
                "Bukayo Saka",
                null,
                clubId,
                "Arsenal",
                seasonId,
                "2024/25",
                30,
                2450,
                8,
                5,
                new BigDecimal("7.20"),
                new BigDecimal("4.10"),
                Position.RW,
                null,
                null
        );
    }

    private static void setId(Object entity, UUID id) {
        try {
            var idField = entity.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
