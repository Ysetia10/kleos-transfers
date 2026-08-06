package com.kleos.transfers.clubseason.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.club.repository.ClubRepository;
import com.kleos.transfers.clubseason.dto.ClubSeasonResponse;
import com.kleos.transfers.clubseason.dto.CreateClubSeasonRequest;
import com.kleos.transfers.clubseason.dto.UpdateClubSeasonRequest;
import com.kleos.transfers.clubseason.entity.ClubSeason;
import com.kleos.transfers.clubseason.mapper.ClubSeasonMapper;
import com.kleos.transfers.clubseason.repository.ClubSeasonRepository;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.domain.Confederation;
import com.kleos.transfers.domain.TournamentType;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.season.repository.SeasonRepository;
import com.kleos.transfers.tournament.entity.Tournament;
import com.kleos.transfers.tournament.repository.TournamentRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClubSeasonServiceImplTest {

    @Mock
    private ClubSeasonRepository clubSeasonRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private ClubSeasonMapper clubSeasonMapper;

    @Mock
    private BulkImporter bulkImporter;

    @InjectMocks
    private ClubSeasonServiceImpl clubSeasonService;

    @Test
    void createsClubSeasonFromResolvedIdentities() {
        UUID clubId = UUID.randomUUID();
        UUID seasonId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        CreateClubSeasonRequest request = new CreateClubSeasonRequest(clubId, seasonId, tournamentId);

        Club club = club(clubId);
        Season season = season(seasonId);
        Tournament tournament = tournament(tournamentId);
        ClubSeason entity = new ClubSeason(club, season, tournament);
        ClubSeasonResponse expected = response(clubId, seasonId, tournamentId);

        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season));
        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(clubSeasonMapper.toEntity(club, season, tournament)).thenReturn(entity);
        when(clubSeasonRepository.save(entity)).thenReturn(entity);
        when(clubSeasonMapper.toResponse(entity)).thenReturn(expected);

        assertThat(clubSeasonService.create(request)).isSameAs(expected);
        verify(clubSeasonRepository).save(entity);
    }

    @Test
    void rejectsUnknownClub() {
        UUID clubId = UUID.randomUUID();
        when(clubRepository.findById(clubId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubSeasonService.create(
                new CreateClubSeasonRequest(clubId, UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(clubId.toString());
    }

    @Test
    void softDeletesExistingClubSeason() {
        UUID id = UUID.randomUUID();
        Club club = club(UUID.randomUUID());
        Season season = season(UUID.randomUUID());
        ClubSeason clubSeason = new ClubSeason(club, season, tournament(UUID.randomUUID()));
        try {
            var idField = clubSeason.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(clubSeason, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }

        when(clubSeasonRepository.findById(id)).thenReturn(Optional.of(clubSeason));

        clubSeasonService.softDelete(id);

        assertThat(clubSeason.isDeleted()).isTrue();
        assertThat(clubSeason.getUniquenessKey()).endsWith("#" + id);
    }

    @Test
    void updatesClubSeasonLinks() {
        UUID id = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        UUID seasonId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        UpdateClubSeasonRequest request = new UpdateClubSeasonRequest(clubId, seasonId, tournamentId);

        Club club = club(clubId);
        Season season = season(seasonId);
        Tournament tournament = tournament(tournamentId);
        ClubSeason clubSeason = new ClubSeason(
                club(UUID.randomUUID()), season(UUID.randomUUID()), tournament(UUID.randomUUID()));
        ClubSeasonResponse expected = response(clubId, seasonId, tournamentId);

        when(clubSeasonRepository.findById(id)).thenReturn(Optional.of(clubSeason));
        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season));
        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(clubSeasonMapper.toResponse(clubSeason)).thenReturn(expected);

        assertThat(clubSeasonService.update(id, request)).isSameAs(expected);
        assertThat(clubSeason.getClub()).isSameAs(club);
        assertThat(clubSeason.getSeason()).isSameAs(season);
        assertThat(clubSeason.getTournament()).isSameAs(tournament);
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

    private Tournament tournament(UUID id) {
        Tournament tournament = new Tournament(
                "Premier League", "EPL", Confederation.UEFA, TournamentType.LEAGUE, "ENG");
        setId(tournament, id);
        return tournament;
    }

    private ClubSeasonResponse response(UUID clubId, UUID seasonId, UUID tournamentId) {
        return new ClubSeasonResponse(
                UUID.randomUUID(),
                clubId,
                "Arsenal",
                seasonId,
                "2024/25",
                tournamentId,
                "Premier League",
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
