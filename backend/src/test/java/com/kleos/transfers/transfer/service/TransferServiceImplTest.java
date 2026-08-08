package com.kleos.transfers.transfer.service;

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
import com.kleos.transfers.domain.TransferType;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.player.repository.PlayerRepository;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.season.repository.SeasonRepository;
import com.kleos.transfers.transfer.dto.CreateTransferRequest;
import com.kleos.transfers.transfer.dto.TransferResponse;
import com.kleos.transfers.transfer.entity.Transfer;
import com.kleos.transfers.transfer.mapper.TransferMapper;
import com.kleos.transfers.transfer.repository.TransferRepository;
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
class TransferServiceImplTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private TransferMapper transferMapper;

    @Mock
    private BulkImporter bulkImporter;

    @InjectMocks
    private TransferServiceImpl transferService;

    @Test
    void createsTransferFromResolvedIdentities() {
        UUID playerId = UUID.randomUUID();
        UUID fromClubId = UUID.randomUUID();
        UUID toClubId = UUID.randomUUID();
        UUID seasonId = UUID.randomUUID();
        CreateTransferRequest request = new CreateTransferRequest(
                playerId,
                fromClubId,
                toClubId,
                seasonId,
                LocalDate.of(2024, 8, 12),
                new BigDecimal("50000000"),
                TransferType.PERMANENT
        );

        Player player = player(playerId);
        Club fromClub = club(fromClubId, "Dortmund");
        Club toClub = club(toClubId, "Real Madrid");
        Season season = season(seasonId);
        Transfer entity = new Transfer(
                player, fromClub, toClub, season, request.transferDate(), request.feeEur(), request.type());
        TransferResponse expected = response(playerId, fromClubId, toClubId, seasonId);

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
        when(clubRepository.findById(fromClubId)).thenReturn(Optional.of(fromClub));
        when(clubRepository.findById(toClubId)).thenReturn(Optional.of(toClub));
        when(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season));
        when(transferMapper.toEntity(player, fromClub, toClub, season, request)).thenReturn(entity);
        when(transferRepository.save(entity)).thenReturn(entity);
        when(transferMapper.toResponse(entity)).thenReturn(expected);

        assertThat(transferService.create(request)).isSameAs(expected);
        verify(transferRepository).save(entity);
    }

    @Test
    void createsFreeAgentSigningWithoutFromClub() {
        UUID playerId = UUID.randomUUID();
        UUID toClubId = UUID.randomUUID();
        UUID seasonId = UUID.randomUUID();
        CreateTransferRequest request = new CreateTransferRequest(
                playerId,
                null,
                toClubId,
                seasonId,
                LocalDate.of(2024, 7, 1),
                null,
                TransferType.FREE
        );

        Player player = player(playerId);
        Club toClub = club(toClubId, "Arsenal");
        Season season = season(seasonId);
        Transfer entity = new Transfer(
                player, null, toClub, season, request.transferDate(), null, TransferType.FREE);
        TransferResponse expected = response(playerId, null, toClubId, seasonId);

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
        when(clubRepository.findById(toClubId)).thenReturn(Optional.of(toClub));
        when(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season));
        when(transferMapper.toEntity(player, null, toClub, season, request)).thenReturn(entity);
        when(transferRepository.save(entity)).thenReturn(entity);
        when(transferMapper.toResponse(entity)).thenReturn(expected);

        assertThat(transferService.create(request)).isSameAs(expected);
    }

    @Test
    void rejectsUnknownPlayer() {
        UUID playerId = UUID.randomUUID();
        when(playerRepository.findById(playerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.create(new CreateTransferRequest(
                playerId,
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.of(2024, 7, 1),
                null,
                TransferType.FREE)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(playerId.toString());
    }

    @Test
    void softDeletesExistingTransfer() {
        UUID id = UUID.randomUUID();
        Transfer transfer = new Transfer(
                player(UUID.randomUUID()),
                club(UUID.randomUUID(), "Dortmund"),
                club(UUID.randomUUID(), "Real Madrid"),
                season(UUID.randomUUID()),
                LocalDate.of(2024, 8, 12),
                new BigDecimal("50000000"),
                TransferType.PERMANENT
        );
        setId(transfer, id);

        when(transferRepository.findById(id)).thenReturn(Optional.of(transfer));

        transferService.softDelete(id);

        assertThat(transfer.isDeleted()).isTrue();
        assertThat(transfer.getUniquenessKey()).endsWith("#" + id);
    }

    private Player player(UUID id) {
        Player player = new Player(
                "Jude Bellingham",
                LocalDate.of(2003, 6, 29),
                DateOfBirthPrecision.DAY,
                "ENG",
                186,
                PreferredFoot.RIGHT,
                Position.CM
        );
        setId(player, id);
        return player;
    }

    private Club club(UUID id, String name) {
        Club club = new Club(name, name.substring(0, Math.min(3, name.length())).toUpperCase(), "ENG", 1900);
        setId(club, id);
        return club;
    }

    private Season season(UUID id) {
        Season season = new Season("2024/25", LocalDate.of(2024, 7, 1), LocalDate.of(2025, 6, 30));
        setId(season, id);
        return season;
    }

    private TransferResponse response(UUID playerId, UUID fromClubId, UUID toClubId, UUID seasonId) {
        return new TransferResponse(
                UUID.randomUUID(),
                playerId,
                "Jude Bellingham",
                fromClubId,
                fromClubId == null ? null : "Dortmund",
                toClubId,
                "Real Madrid",
                seasonId,
                "2024/25",
                LocalDate.of(2024, 8, 12),
                new BigDecimal("50000000"),
                TransferType.PERMANENT,
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
