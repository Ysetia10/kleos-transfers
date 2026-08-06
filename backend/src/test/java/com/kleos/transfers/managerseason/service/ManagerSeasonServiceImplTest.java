package com.kleos.transfers.managerseason.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.club.repository.ClubRepository;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.manager.entity.Manager;
import com.kleos.transfers.manager.repository.ManagerRepository;
import com.kleos.transfers.managerseason.dto.CreateManagerSeasonRequest;
import com.kleos.transfers.managerseason.dto.ManagerSeasonResponse;
import com.kleos.transfers.managerseason.dto.UpdateManagerSeasonRequest;
import com.kleos.transfers.managerseason.entity.ManagerSeason;
import com.kleos.transfers.managerseason.mapper.ManagerSeasonMapper;
import com.kleos.transfers.managerseason.repository.ManagerSeasonRepository;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.season.repository.SeasonRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagerSeasonServiceImplTest {

    @Mock
    private ManagerSeasonRepository managerSeasonRepository;

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private ManagerSeasonMapper managerSeasonMapper;

    @Mock
    private BulkImporter bulkImporter;

    @InjectMocks
    private ManagerSeasonServiceImpl managerSeasonService;

    @Test
    void createsManagerSeasonFromResolvedIdentities() {
        UUID managerId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        UUID seasonId = UUID.randomUUID();
        CreateManagerSeasonRequest request = new CreateManagerSeasonRequest(managerId, clubId, seasonId);

        Manager manager = manager(managerId);
        Club club = club(clubId);
        Season season = season(seasonId);
        ManagerSeason entity = new ManagerSeason(manager, club, season);
        ManagerSeasonResponse expected = response(managerId, clubId, seasonId);

        when(managerRepository.findById(managerId)).thenReturn(Optional.of(manager));
        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season));
        when(managerSeasonMapper.toEntity(manager, club, season)).thenReturn(entity);
        when(managerSeasonRepository.save(entity)).thenReturn(entity);
        when(managerSeasonMapper.toResponse(entity)).thenReturn(expected);

        assertThat(managerSeasonService.create(request)).isSameAs(expected);
        verify(managerSeasonRepository).save(entity);
    }

    @Test
    void rejectsUnknownManager() {
        UUID managerId = UUID.randomUUID();
        when(managerRepository.findById(managerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> managerSeasonService.create(
                new CreateManagerSeasonRequest(managerId, UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(managerId.toString());
    }

    @Test
    void softDeletesExistingManagerSeason() {
        UUID id = UUID.randomUUID();
        ManagerSeason managerSeason = new ManagerSeason(
                manager(UUID.randomUUID()), club(UUID.randomUUID()), season(UUID.randomUUID()));
        setId(managerSeason, id);

        when(managerSeasonRepository.findById(id)).thenReturn(Optional.of(managerSeason));

        managerSeasonService.softDelete(id);

        assertThat(managerSeason.isDeleted()).isTrue();
        assertThat(managerSeason.getUniquenessKey()).endsWith("#" + id);
    }

    @Test
    void updatesManagerSeasonLinks() {
        UUID id = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        UUID seasonId = UUID.randomUUID();
        UpdateManagerSeasonRequest request = new UpdateManagerSeasonRequest(managerId, clubId, seasonId);

        Manager manager = manager(managerId);
        Club club = club(clubId);
        Season season = season(seasonId);
        ManagerSeason managerSeason = new ManagerSeason(
                manager(UUID.randomUUID()), club(UUID.randomUUID()), season(UUID.randomUUID()));
        ManagerSeasonResponse expected = response(managerId, clubId, seasonId);

        when(managerSeasonRepository.findById(id)).thenReturn(Optional.of(managerSeason));
        when(managerRepository.findById(managerId)).thenReturn(Optional.of(manager));
        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season));
        when(managerSeasonMapper.toResponse(managerSeason)).thenReturn(expected);

        assertThat(managerSeasonService.update(id, request)).isSameAs(expected);
        assertThat(managerSeason.getManager()).isSameAs(manager);
        assertThat(managerSeason.getClub()).isSameAs(club);
        assertThat(managerSeason.getSeason()).isSameAs(season);
    }

    private Manager manager(UUID id) {
        Manager manager = new Manager("Mikel Arteta", LocalDate.of(1982, 3, 26), "ESP");
        setId(manager, id);
        return manager;
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

    private ManagerSeasonResponse response(UUID managerId, UUID clubId, UUID seasonId) {
        return new ManagerSeasonResponse(
                UUID.randomUUID(),
                managerId,
                "Mikel Arteta",
                clubId,
                "Arsenal",
                seasonId,
                "2024/25",
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
