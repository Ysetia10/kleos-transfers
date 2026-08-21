package com.kleos.transfers.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kleos.transfers.club.dto.ClubResponse;
import com.kleos.transfers.club.dto.CreateClubRequest;
import com.kleos.transfers.club.dto.UpdateClubRequest;
import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.club.mapper.ClubMapper;
import com.kleos.transfers.club.repository.ClubRepository;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.exception.ConflictException;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.domain.DateOfBirthPrecision;
import com.kleos.transfers.domain.Position;
import com.kleos.transfers.domain.PreferredFoot;
import com.kleos.transfers.domain.RecruitmentSignal;
import com.kleos.transfers.domain.TransferStatus;
import com.kleos.transfers.domain.TransferType;
import com.kleos.transfers.managerseason.repository.ManagerSeasonRepository;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.playerseason.dto.PlayerSeasonResponse;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import com.kleos.transfers.playerseason.mapper.PlayerSeasonMapper;
import com.kleos.transfers.playerseason.repository.PlayerSeasonRepository;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.season.repository.SeasonRepository;
import com.kleos.transfers.transfer.entity.Transfer;
import com.kleos.transfers.transfer.mapper.TransferMapper;
import com.kleos.transfers.transfer.repository.TransferRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ClubServiceImplTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMapper clubMapper;

    @Mock
    private BulkImporter bulkImporter;

    @Mock
    private PlayerSeasonRepository playerSeasonRepository;

    @Mock
    private PlayerSeasonMapper playerSeasonMapper;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private ManagerSeasonRepository managerSeasonRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransferMapper transferMapper;

    @InjectMocks
    private ClubServiceImpl clubService;

    @Test
    void createsClubIdentity() {
        CreateClubRequest request = createRequest();
        Club club = club();
        ClubResponse expected = response();

        when(clubRepository.existsByNameNormalizedAndCountryCode("fc barcelona", "ESP")).thenReturn(false);
        when(clubMapper.toEntity(request)).thenReturn(club);
        when(clubRepository.save(club)).thenReturn(club);
        when(managerSeasonRepository.findCurrentManagersByClubIds(anyCollection())).thenReturn(List.of());
        when(clubMapper.toResponse(
                eq(club), isNull(), isNull(), isNull(), isNull(), eq(false), any()
        )).thenReturn(expected);

        assertThat(clubService.create(request)).isSameAs(expected);
        verify(clubRepository).save(club);
    }

    @Test
    void rejectsDuplicateClubNaturalKeyOnCreate() {
        CreateClubRequest request = createRequest();
        when(clubRepository.existsByNameNormalizedAndCountryCode("fc barcelona", "ESP")).thenReturn(true);

        assertThatThrownBy(() -> clubService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void returnsPagedClubs() {
        Pageable pageable = PageRequest.of(0, 20);
        Club club = club();
        ClubResponse expected = response();
        when(clubRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(club)));
        when(managerSeasonRepository.findCurrentManagersByClubIds(anyCollection())).thenReturn(List.of());
        when(clubMapper.toResponse(
                eq(club), isNull(), isNull(), isNull(), isNull(), eq(false), any()
        )).thenReturn(expected);

        Page<ClubResponse> actual = clubService.findAll(null, pageable);

        assertThat(actual.getContent()).containsExactly(expected);
    }

    @Test
    void returnsClubById() {
        UUID id = UUID.randomUUID();
        Club club = club();
        ClubResponse expected = response();

        when(clubRepository.findById(id)).thenReturn(Optional.of(club));
        when(managerSeasonRepository.findCurrentManagersByClubIds(anyCollection())).thenReturn(List.of());
        when(clubMapper.toResponse(
                eq(club), isNull(), isNull(), isNull(), isNull(), eq(false), any()
        )).thenReturn(expected);

        assertThat(clubService.findById(id)).isSameAs(expected);
    }

    @Test
    void updatesExistingClubIdentity() {
        UUID id = UUID.randomUUID();
        Club club = club();
        UpdateClubRequest request = updateRequest();
        ClubResponse expected = response();

        when(clubRepository.findById(id)).thenReturn(Optional.of(club));
        when(clubRepository.existsByNameNormalizedAndCountryCodeAndIdNot("fc barcelona", "ESP", id))
                .thenReturn(false);
        when(managerSeasonRepository.findCurrentManagersByClubIds(anyCollection())).thenReturn(List.of());
        when(clubMapper.toResponse(
                eq(club), isNull(), isNull(), isNull(), isNull(), eq(false), any()
        )).thenReturn(expected);

        assertThat(clubService.update(id, request)).isSameAs(expected);
        verify(clubMapper).updateEntity(club, request);
    }

    @Test
    void softDeletesExistingClub() {
        UUID id = UUID.randomUUID();
        Club club = club();
        when(clubRepository.findById(id)).thenReturn(Optional.of(club));

        clubService.softDelete(id);

        assertThat(club.isDeleted()).isTrue();
        assertThat(club.getDeletedAt()).isNotNull();
    }

    @Test
    void rejectsUnknownClub() {
        UUID id = UUID.randomUUID();
        when(clubRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void projectsEmptyUpcomingSquadFromPriorRosterMinusOutsPlusIns() {
        UUID clubId = UUID.randomUUID();
        UUID priorSeasonId = UUID.randomUUID();
        UUID targetSeasonId = UUID.randomUUID();
        UUID stayerId = UUID.randomUUID();
        UUID leaverId = UUID.randomUUID();
        UUID arrivalId = UUID.randomUUID();

        Club club = clubWithId(clubId, "FC Barcelona");
        Season prior = seasonWithId(priorSeasonId, "2025/26", LocalDate.of(2025, 7, 1), LocalDate.of(2026, 6, 30));
        Season target = seasonWithId(targetSeasonId, "2026/27", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30));

        Player stayer = playerWithId(stayerId, "Pedri");
        Player leaver = playerWithId(leaverId, "Departed");
        Player arrival = playerWithId(arrivalId, "Signing");

        PlayerSeason stayerRow = playerSeason(stayer, club, prior, 2000);
        PlayerSeason leaverRow = playerSeason(leaver, club, prior, 1500);
        PlayerSeason arrivalPrior = playerSeason(
                arrival,
                clubWithId(UUID.randomUUID(), "Other Club"),
                prior,
                1800
        );

        Transfer out = new Transfer(
                leaver,
                club,
                clubWithId(UUID.randomUUID(), "Away"),
                target,
                LocalDate.of(2026, 7, 10),
                null,
                TransferType.PERMANENT,
                TransferStatus.COMPLETED,
                null,
                null
        );
        Transfer in = new Transfer(
                arrival,
                clubWithId(UUID.randomUUID(), "Seller"),
                club,
                target,
                LocalDate.of(2026, 7, 15),
                null,
                TransferType.PERMANENT,
                TransferStatus.ANNOUNCED,
                null,
                null
        );

        PlayerSeasonResponse stayerProjected = responseRow(stayerId, "Pedri", clubId, targetSeasonId, 2000);
        PlayerSeasonResponse arrivalProjected = responseRow(arrivalId, "Signing", clubId, targetSeasonId, 1800);

        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(seasonRepository.findById(targetSeasonId)).thenReturn(Optional.of(target));
        when(playerSeasonRepository.findByClubIdAndSeasonId(clubId, targetSeasonId)).thenReturn(List.of());
        when(seasonRepository.findFirstByStartDateLessThanOrderByStartDateDesc(target.getStartDate()))
                .thenReturn(Optional.of(prior));
        when(playerSeasonRepository.findByClubIdAndSeasonId(clubId, priorSeasonId))
                .thenReturn(List.of(stayerRow, leaverRow));
        when(transferRepository.findBySeasonIdAndClubIdAndStatusIn(
                eq(targetSeasonId),
                eq(clubId),
                anyCollection()
        )).thenReturn(List.of(out, in));
        when(playerSeasonRepository.findHistoryByPlayerIdBefore(arrivalId, target.getStartDate()))
                .thenReturn(List.of(arrivalPrior));
        when(transferMapper.toMoveSummary(in)).thenReturn(null);
        when(playerSeasonMapper.toProjectedResponse(stayerRow, club, target, null)).thenReturn(stayerProjected);
        when(playerSeasonMapper.toProjectedArrival(arrival, club, target, arrivalPrior, null))
                .thenReturn(arrivalProjected);

        List<PlayerSeasonResponse> squad = clubService.findSquad(clubId, targetSeasonId);

        assertThat(squad).containsExactly(stayerProjected, arrivalProjected);
    }

    private CreateClubRequest createRequest() {
        return new CreateClubRequest("FC Barcelona", "Barcelona", "ESP", 1899, null);
    }

    private UpdateClubRequest updateRequest() {
        return new UpdateClubRequest("FC Barcelona", "Barça", "ESP", 1899, null);
    }

    private Club club() {
        return new Club("FC Barcelona", "Barcelona", "ESP", 1899);
    }

    private Club clubWithId(UUID id, String name) {
        Club club = new Club(name, name.substring(0, Math.min(3, name.length())).toUpperCase(), "ESP", 1899);
        setId(club, id);
        return club;
    }

    private Season seasonWithId(UUID id, String label, LocalDate start, LocalDate end) {
        Season season = new Season(label, start, end);
        setId(season, id);
        return season;
    }

    private Player playerWithId(UUID id, String name) {
        Player player = new Player(
                name,
                LocalDate.of(2000, 1, 1),
                DateOfBirthPrecision.DAY,
                "ESP",
                180,
                PreferredFoot.RIGHT,
                Position.CM
        );
        setId(player, id);
        return player;
    }

    private PlayerSeason playerSeason(Player player, Club club, Season season, int minutes) {
        PlayerSeason row = new PlayerSeason(
                player,
                club,
                season,
                30,
                minutes,
                5,
                5,
                BigDecimal.ONE,
                BigDecimal.ONE,
                Position.CM
        );
        setId(row, UUID.randomUUID());
        return row;
    }

    private PlayerSeasonResponse responseRow(
            UUID playerId,
            String playerName,
            UUID clubId,
            UUID seasonId,
            int minutes
    ) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new PlayerSeasonResponse(
                UUID.randomUUID(),
                playerId,
                playerName,
                null,
                clubId,
                "FC Barcelona",
                seasonId,
                "2026/27",
                30,
                minutes,
                5,
                5,
                BigDecimal.ONE,
                BigDecimal.ONE,
                Position.CM,
                now,
                now,
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

    private ClubResponse response() {
        return new ClubResponse(
                UUID.randomUUID(),
                "FC Barcelona",
                "Barcelona",
                "ESP",
                1899,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                java.math.BigDecimal.valueOf(38.0),
                RecruitmentSignal.UNKNOWN,
                ClubFitIndexCalculator.VERSION,
                null,
                null
        );
    }
}
