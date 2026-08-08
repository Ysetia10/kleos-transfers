package com.kleos.transfers.contract.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.club.repository.ClubRepository;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.contract.dto.ContractResponse;
import com.kleos.transfers.contract.dto.CreateContractRequest;
import com.kleos.transfers.contract.entity.Contract;
import com.kleos.transfers.contract.mapper.ContractMapper;
import com.kleos.transfers.contract.repository.ContractRepository;
import com.kleos.transfers.domain.DateOfBirthPrecision;
import com.kleos.transfers.domain.Position;
import com.kleos.transfers.domain.PreferredFoot;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.player.repository.PlayerRepository;
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
class ContractServiceImplTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ContractMapper contractMapper;

    @Mock
    private BulkImporter bulkImporter;

    @InjectMocks
    private ContractServiceImpl contractService;

    @Test
    void createsContractFromResolvedIdentities() {
        UUID playerId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        CreateContractRequest request = new CreateContractRequest(
                playerId,
                clubId,
                LocalDate.of(2023, 7, 1),
                LocalDate.of(2029, 6, 30),
                new BigDecimal("1000000000")
        );

        Player player = player(playerId);
        Club club = club(clubId, "Real Madrid");
        Contract entity = new Contract(
                player, club, request.startDate(), request.endDate(), request.releaseClauseEur());
        ContractResponse expected = response(playerId, clubId);

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(contractMapper.toEntity(player, club, request)).thenReturn(entity);
        when(contractRepository.save(entity)).thenReturn(entity);
        when(contractMapper.toResponse(entity)).thenReturn(expected);

        assertThat(contractService.create(request)).isSameAs(expected);
        verify(contractRepository).save(entity);
    }

    @Test
    void rejectsUnknownClub() {
        UUID playerId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player(playerId)));
        when(clubRepository.findById(clubId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contractService.create(new CreateContractRequest(
                playerId,
                clubId,
                LocalDate.of(2023, 7, 1),
                LocalDate.of(2029, 6, 30),
                null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(clubId.toString());
    }

    @Test
    void softDeleteFreesTheUniquenessSlot() {
        UUID id = UUID.randomUUID();
        Contract contract = new Contract(
                player(UUID.randomUUID()),
                club(UUID.randomUUID(), "Real Madrid"),
                LocalDate.of(2023, 7, 1),
                LocalDate.of(2029, 6, 30),
                null
        );
        setId(contract, id);

        when(contractRepository.findById(id)).thenReturn(Optional.of(contract));

        contractService.softDelete(id);

        assertThat(contract.isDeleted()).isTrue();
        assertThat(contract.getUniquenessKey()).endsWith("#" + id);
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
        Club club = new Club(name, name.substring(0, Math.min(3, name.length())).toUpperCase(), "ESP", 1902);
        setId(club, id);
        return club;
    }

    private ContractResponse response(UUID playerId, UUID clubId) {
        return new ContractResponse(
                UUID.randomUUID(),
                playerId,
                "Jude Bellingham",
                clubId,
                "Real Madrid",
                LocalDate.of(2023, 7, 1),
                LocalDate.of(2029, 6, 30),
                new BigDecimal("1000000000"),
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
