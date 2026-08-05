package com.kleos.transfers.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kleos.transfers.domain.Position;
import com.kleos.transfers.domain.PreferredFoot;
import com.kleos.transfers.player.dto.CreatePlayerRequest;
import com.kleos.transfers.player.dto.PlayerResponse;
import com.kleos.transfers.player.dto.UpdatePlayerRequest;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.player.mapper.PlayerMapper;
import com.kleos.transfers.player.repository.PlayerRepository;
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
    private PlayerMapper playerMapper;

    @InjectMocks
    private PlayerServiceImpl playerService;

    @Test
    void createsPlayerIdentity() {
        CreatePlayerRequest request = createRequest();
        Player player = player();
        PlayerResponse expected = response();

        when(playerMapper.toEntity(request)).thenReturn(player);
        when(playerRepository.save(player)).thenReturn(player);
        when(playerMapper.toResponse(player)).thenReturn(expected);

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
        when(playerMapper.toResponse(player)).thenReturn(expected);

        Page<PlayerResponse> actual = playerService.findAll(pageable);

        assertThat(actual.getContent()).containsExactly(expected);
    }

    @Test
    void returnsPlayerById() {
        UUID id = UUID.randomUUID();
        Player player = player();
        PlayerResponse expected = response();

        when(playerRepository.findById(id)).thenReturn(Optional.of(player));
        when(playerMapper.toResponse(player)).thenReturn(expected);

        assertThat(playerService.findById(id)).isSameAs(expected);
    }

    @Test
    void updatesExistingPlayerIdentity() {
        UUID id = UUID.randomUUID();
        Player player = player();
        UpdatePlayerRequest request = updateRequest();
        PlayerResponse expected = response();

        when(playerRepository.findById(id)).thenReturn(Optional.of(player));
        when(playerMapper.toResponse(player)).thenReturn(expected);

        PlayerResponse actual = playerService.update(id, request);

        assertThat(actual).isSameAs(expected);
        verify(playerMapper).updateEntity(player, request);
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
        when(playerRepository.findById(id)).thenReturn(Optional.of(player));

        playerService.softDelete(id);

        assertThat(player.isDeleted()).isTrue();
        assertThat(player.getDeletedAt()).isNotNull();
    }

    private CreatePlayerRequest createRequest() {
        return new CreatePlayerRequest(
                "Test Player",
                LocalDate.of(2000, 1, 1),
                "ENG",
                180,
                PreferredFoot.RIGHT,
                Position.CM
        );
    }

    private UpdatePlayerRequest updateRequest() {
        return new UpdatePlayerRequest(
                "Updated Player",
                LocalDate.of(2000, 1, 1),
                "NED",
                181,
                PreferredFoot.LEFT,
                Position.CAM
        );
    }

    private Player player() {
        return new Player(
                "Test Player",
                LocalDate.of(2000, 1, 1),
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
                "ENG",
                180,
                PreferredFoot.RIGHT,
                Position.CM,
                null,
                null
        );
    }
}
