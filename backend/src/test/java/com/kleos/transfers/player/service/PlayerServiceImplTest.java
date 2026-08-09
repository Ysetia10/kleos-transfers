package com.kleos.transfers.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kleos.transfers.domain.DateOfBirthPrecision;
import com.kleos.transfers.domain.Position;
import com.kleos.transfers.domain.PreferredFoot;
import com.kleos.transfers.player.dto.CreatePlayerRequest;
import com.kleos.transfers.player.dto.PlayerResponse;
import com.kleos.transfers.player.dto.UpdatePlayerRequest;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.exception.ConflictException;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.player.mapper.PlayerMapper;
import com.kleos.transfers.player.repository.PlayerRepository;
import com.kleos.transfers.playerseason.repository.PlayerSeasonRepository;
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
class PlayerServiceImplTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerSeasonRepository playerSeasonRepository;

    @Mock
    private PlayerMapper playerMapper;

    @Mock
    private BulkImporter bulkImporter;

    @InjectMocks
    private PlayerServiceImpl playerService;

    @Test
    void createsPlayerIdentity() {
        CreatePlayerRequest request = createRequest();
        Player player = player();
        PlayerResponse expected = response();

        when(playerRepository.existsByFullNameNormalizedAndDateOfBirthAndNationality(
                "test player", request.dateOfBirth(), "ENG")).thenReturn(false);
        when(playerMapper.toEntity(request)).thenReturn(player);
        when(playerRepository.save(player)).thenReturn(player);
        when(playerSeasonRepository.findLatestClubsByPlayerIds(any())).thenReturn(List.of());
        when(playerMapper.toResponse(eq(player), isNull())).thenReturn(expected);

        PlayerResponse actual = playerService.create(request);

        assertThat(actual).isSameAs(expected);
        verify(playerRepository).save(player);
    }

    @Test
    void returnsPagedPlayers() {
        Pageable pageable = PageRequest.of(0, 20);
        Player player = player();
        PlayerResponse expected = response();
        when(playerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(player)));
        when(playerSeasonRepository.findLatestClubsByPlayerIds(any())).thenReturn(List.of());
        when(playerMapper.toResponse(eq(player), isNull())).thenReturn(expected);

        Page<PlayerResponse> actual = playerService.findAll(null, null, null, null, null, pageable);

        assertThat(actual.getContent()).containsExactly(expected);
    }

    @Test
    void searchesPlayersByName() {
        Pageable pageable = PageRequest.of(0, 20);
        Player player = player();
        PlayerResponse expected = response();
        when(playerRepository.searchFiltered(
                        eq(true),
                        eq("rice"),
                        eq(false),
                        any(),
                        eq(false),
                        any(),
                        eq(false),
                        any(),
                        eq(false),
                        any(),
                        eq(false),
                        eq(""),
                        eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(player)));
        when(playerSeasonRepository.findLatestClubsByPlayerIds(any())).thenReturn(List.of());
        when(playerMapper.toResponse(eq(player), isNull())).thenReturn(expected);

        Page<PlayerResponse> actual = playerService.findAll("rice", null, null, null, null, pageable);

        assertThat(actual.getContent()).containsExactly(expected);
    }

    @Test
    void filtersPlayersByPositionGroup() {
        Pageable pageable = PageRequest.of(0, 20);
        Player player = player();
        PlayerResponse expected = response();
        when(playerRepository.searchFiltered(
                        eq(false),
                        eq(""),
                        eq(false),
                        any(),
                        eq(true),
                        any(),
                        eq(false),
                        any(),
                        eq(false),
                        any(),
                        eq(false),
                        eq(""),
                        eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(player)));
        when(playerSeasonRepository.findLatestClubsByPlayerIds(any())).thenReturn(List.of());
        when(playerMapper.toResponse(eq(player), isNull())).thenReturn(expected);

        Page<PlayerResponse> actual = playerService.findAll(null, "MID", null, null, null, pageable);

        assertThat(actual.getContent()).containsExactly(expected);
    }

    @Test
    void returnsPlayerById() {
        UUID id = UUID.randomUUID();
        Player player = player();
        PlayerResponse expected = response();

        when(playerRepository.findById(id)).thenReturn(Optional.of(player));
        when(playerSeasonRepository.findLatestClubsByPlayerIds(any())).thenReturn(List.of());
        when(playerMapper.toResponse(eq(player), isNull())).thenReturn(expected);

        assertThat(playerService.findById(id)).isSameAs(expected);
    }

    @Test
    void updatesExistingPlayerIdentity() {
        UUID id = UUID.randomUUID();
        Player player = player();
        UpdatePlayerRequest request = updateRequest();
        PlayerResponse expected = response();

        when(playerRepository.findById(id)).thenReturn(Optional.of(player));
        when(playerRepository.existsByFullNameNormalizedAndDateOfBirthAndNationalityAndIdNot(
                "updated player", request.dateOfBirth(), "NED", id)).thenReturn(false);
        when(playerSeasonRepository.findLatestClubsByPlayerIds(any())).thenReturn(List.of());
        when(playerMapper.toResponse(eq(player), isNull())).thenReturn(expected);

        PlayerResponse actual = playerService.update(id, request);

        assertThat(actual).isSameAs(expected);
        verify(playerMapper).updateEntity(player, request);
    }

    @Test
    void rejectsDuplicateNaturalKeyOnCreate() {
        CreatePlayerRequest request = createRequest();
        when(playerRepository.existsByFullNameNormalizedAndDateOfBirthAndNationality(
                "test player", request.dateOfBirth(), "ENG")).thenReturn(true);

        assertThatThrownBy(() -> playerService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void rejectsUnknownPlayer() {
        UUID id = UUID.randomUUID();
        when(playerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void softDeletesExistingPlayer() {
        UUID id = UUID.randomUUID();
        Player player = player();
        setId(player, id);
        when(playerRepository.findById(id)).thenReturn(Optional.of(player));

        playerService.softDelete(id);

        assertThat(player.isDeleted()).isTrue();
        assertThat(player.getDeletedAt()).isNotNull();
        assertThat(player.getFullNameNormalized()).endsWith("#" + id);
    }

    private static void setId(Player player, UUID id) {
        try {
            var idField = Player.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(player, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private CreatePlayerRequest createRequest() {
        return new CreatePlayerRequest(
                "Test Player",
                LocalDate.of(2000, 1, 1),
                DateOfBirthPrecision.DAY,
                "ENG",
                180,
                PreferredFoot.RIGHT,
                Position.CM,
                null
        );
    }

    private UpdatePlayerRequest updateRequest() {
        return new UpdatePlayerRequest(
                "Updated Player",
                LocalDate.of(2000, 1, 1),
                DateOfBirthPrecision.DAY,
                "NED",
                181,
                PreferredFoot.LEFT,
                Position.CAM,
                null
        );
    }

    private Player player() {
        return new Player(
                "Test Player",
                LocalDate.of(2000, 1, 1),
                DateOfBirthPrecision.DAY,
                "ENG",
                180,
                PreferredFoot.RIGHT,
                Position.CM
        );
    }

    private PlayerResponse response() {
        return new PlayerResponse(
                UUID.randomUUID(),
                "Test Player",
                LocalDate.of(2000, 1, 1),
                DateOfBirthPrecision.DAY,
                26,
                "ENG",
                180,
                PreferredFoot.RIGHT,
                Position.CM,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
