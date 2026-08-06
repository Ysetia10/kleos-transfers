package com.kleos.transfers.tournament.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.domain.Confederation;
import com.kleos.transfers.domain.TournamentType;
import com.kleos.transfers.tournament.dto.CreateTournamentRequest;
import com.kleos.transfers.tournament.dto.TournamentResponse;
import com.kleos.transfers.tournament.dto.UpdateTournamentRequest;
import com.kleos.transfers.tournament.entity.Tournament;
import com.kleos.transfers.tournament.mapper.TournamentMapper;
import com.kleos.transfers.tournament.repository.TournamentRepository;
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
class TournamentServiceImplTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentMapper tournamentMapper;

    @Mock
    private BulkImporter bulkImporter;

    @InjectMocks
    private TournamentServiceImpl tournamentService;

    @Test
    void createsTournamentIdentity() {
        CreateTournamentRequest request = createRequest();
        Tournament tournament = tournament();
        TournamentResponse expected = response();

        when(tournamentMapper.toEntity(request)).thenReturn(tournament);
        when(tournamentRepository.save(tournament)).thenReturn(tournament);
        when(tournamentMapper.toResponse(tournament)).thenReturn(expected);

        assertThat(tournamentService.create(request)).isSameAs(expected);
        verify(tournamentRepository).save(tournament);
    }

    @Test
    void returnsPagedTournaments() {
        Pageable pageable = PageRequest.of(0, 20);
        Tournament tournament = tournament();
        TournamentResponse expected = response();
        when(tournamentRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(tournament)));
        when(tournamentMapper.toResponse(tournament)).thenReturn(expected);

        Page<TournamentResponse> actual = tournamentService.findAll(pageable);

        assertThat(actual.getContent()).containsExactly(expected);
    }

    @Test
    void returnsTournamentById() {
        UUID id = UUID.randomUUID();
        Tournament tournament = tournament();
        TournamentResponse expected = response();

        when(tournamentRepository.findById(id)).thenReturn(Optional.of(tournament));
        when(tournamentMapper.toResponse(tournament)).thenReturn(expected);

        assertThat(tournamentService.findById(id)).isSameAs(expected);
    }

    @Test
    void updatesExistingTournamentIdentity() {
        UUID id = UUID.randomUUID();
        Tournament tournament = tournament();
        UpdateTournamentRequest request = updateRequest();
        TournamentResponse expected = response();

        when(tournamentRepository.findById(id)).thenReturn(Optional.of(tournament));
        when(tournamentMapper.toResponse(tournament)).thenReturn(expected);

        assertThat(tournamentService.update(id, request)).isSameAs(expected);
        verify(tournamentMapper).updateEntity(tournament, request);
    }

    @Test
    void softDeletesExistingTournamentAndFreesName() {
        UUID id = UUID.randomUUID();
        Tournament tournament = tournament();
        try {
            var idField = tournament.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(tournament, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }

        when(tournamentRepository.findById(id)).thenReturn(Optional.of(tournament));

        tournamentService.softDelete(id);

        assertThat(tournament.isDeleted()).isTrue();
        assertThat(tournament.getNameNormalized()).isEqualTo("premier league#" + id);
    }

    @Test
    void rejectsUnknownTournament() {
        UUID id = UUID.randomUUID();
        when(tournamentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tournamentService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    private CreateTournamentRequest createRequest() {
        return new CreateTournamentRequest(
                "Premier League", "EPL", Confederation.UEFA, TournamentType.LEAGUE, "ENG");
    }

    private UpdateTournamentRequest updateRequest() {
        return new UpdateTournamentRequest(
                "Premier League", "PL", Confederation.UEFA, TournamentType.LEAGUE, "ENG");
    }

    private Tournament tournament() {
        return new Tournament(
                "Premier League", "EPL", Confederation.UEFA, TournamentType.LEAGUE, "ENG");
    }

    private TournamentResponse response() {
        return new TournamentResponse(
                UUID.randomUUID(),
                "Premier League",
                "EPL",
                Confederation.UEFA,
                TournamentType.LEAGUE,
                "ENG",
                null,
                null
        );
    }
}
