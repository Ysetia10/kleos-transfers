package com.kleos.transfers.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kleos.transfers.dto.CreatePlayerRequest;
import com.kleos.transfers.dto.PlayerResponse;
import com.kleos.transfers.dto.UpdatePlayerRequest;
import com.kleos.transfers.entity.Player;
import com.kleos.transfers.entity.enums.Position;
import com.kleos.transfers.entity.enums.PreferredFoot;
import com.kleos.transfers.exception.ResourceNotFoundException;
import com.kleos.transfers.mapper.PlayerMapper;
import com.kleos.transfers.repository.PlayerRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private CreatePlayerRequest createRequest() {
        return new CreatePlayerRequest(
                "Test Player",
                LocalDate.of(2000, 1, 1),
                "IND",
                180,
                PreferredFoot.RIGHT,
                Position.CM
        );
    }

    private UpdatePlayerRequest updateRequest() {
        return new UpdatePlayerRequest(
                "Updated Player",
                LocalDate.of(2000, 1, 1),
                "IND",
                181,
                PreferredFoot.LEFT,
                Position.CAM
        );
    }

    private Player player() {
        return new Player(
                "Test Player",
                LocalDate.of(2000, 1, 1),
                "IND",
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
                "IND",
                180,
                PreferredFoot.RIGHT,
                Position.CM,
                null,
                null
        );
    }
}
