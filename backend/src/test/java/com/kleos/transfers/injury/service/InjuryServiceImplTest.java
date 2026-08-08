package com.kleos.transfers.injury.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.domain.InjurySeverity;
import com.kleos.transfers.domain.DateOfBirthPrecision;
import com.kleos.transfers.domain.Position;
import com.kleos.transfers.domain.PreferredFoot;
import com.kleos.transfers.injury.dto.CreateInjuryRequest;
import com.kleos.transfers.injury.dto.InjuryResponse;
import com.kleos.transfers.injury.entity.Injury;
import com.kleos.transfers.injury.mapper.InjuryMapper;
import com.kleos.transfers.injury.repository.InjuryRepository;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.player.repository.PlayerRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InjuryServiceImplTest {

    @Mock
    private InjuryRepository injuryRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private InjuryMapper injuryMapper;

    @Mock
    private BulkImporter bulkImporter;

    @InjectMocks
    private InjuryServiceImpl injuryService;

    @Test
    void createsInjuryForKnownPlayer() {
        UUID playerId = UUID.randomUUID();
        CreateInjuryRequest request = new CreateInjuryRequest(
                playerId,
                "Hamstring strain",
                InjurySeverity.MODERATE,
                LocalDate.of(2024, 9, 1),
                LocalDate.of(2024, 10, 1)
        );

        Player player = player(playerId);
        Injury entity = new Injury(
                player, request.injuryType(), request.severity(), request.startDate(), request.endDate());
        InjuryResponse expected = response(playerId);

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));
        when(injuryMapper.toEntity(player, request)).thenReturn(entity);
        when(injuryRepository.save(entity)).thenReturn(entity);
        when(injuryMapper.toResponse(entity)).thenReturn(expected);

        assertThat(injuryService.create(request)).isSameAs(expected);
        verify(injuryRepository).save(entity);
    }

    @Test
    void rejectsUnknownPlayer() {
        UUID playerId = UUID.randomUUID();
        when(playerRepository.findById(playerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> injuryService.create(new CreateInjuryRequest(
                playerId,
                "ACL rupture",
                InjurySeverity.SEVERE,
                LocalDate.of(2024, 9, 1),
                null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(playerId.toString());
    }

    @Test
    void derivesDaysOutInclusiveOfBothEnds() {
        Injury injury = new Injury(
                player(UUID.randomUUID()),
                "Hamstring strain",
                InjurySeverity.MINOR,
                LocalDate.of(2024, 9, 1),
                LocalDate.of(2024, 9, 1)
        );

        assertThat(injury.getDaysOut()).isEqualTo(1);
        assertThat(injury.isOngoing()).isFalse();
    }

    @Test
    void reportsOngoingSpellWithoutDaysOut() {
        Injury injury = new Injury(
                player(UUID.randomUUID()),
                "ACL rupture",
                InjurySeverity.SEVERE,
                LocalDate.of(2024, 9, 1),
                null
        );

        assertThat(injury.getDaysOut()).isNull();
        assertThat(injury.isOngoing()).isTrue();
    }

    @Test
    void softDeleteFreesTheUniquenessSlot() {
        UUID id = UUID.randomUUID();
        Injury injury = new Injury(
                player(UUID.randomUUID()),
                "Hamstring strain",
                InjurySeverity.MODERATE,
                LocalDate.of(2024, 9, 1),
                LocalDate.of(2024, 10, 1)
        );
        setId(injury, id);

        when(injuryRepository.findById(id)).thenReturn(Optional.of(injury));

        injuryService.softDelete(id);

        assertThat(injury.isDeleted()).isTrue();
        assertThat(injury.getUniquenessKey()).endsWith("#" + id);
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

    private InjuryResponse response(UUID playerId) {
        return new InjuryResponse(
                UUID.randomUUID(),
                playerId,
                "Jude Bellingham",
                "Hamstring strain",
                InjurySeverity.MODERATE,
                LocalDate.of(2024, 9, 1),
                LocalDate.of(2024, 10, 1),
                31,
                false,
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
