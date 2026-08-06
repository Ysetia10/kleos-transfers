package com.kleos.transfers.season.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.season.dto.CreateSeasonRequest;
import com.kleos.transfers.season.dto.SeasonResponse;
import com.kleos.transfers.season.dto.UpdateSeasonRequest;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.season.mapper.SeasonMapper;
import com.kleos.transfers.season.repository.SeasonRepository;
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
class SeasonServiceImplTest {

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private SeasonMapper seasonMapper;

    @Mock
    private BulkImporter bulkImporter;

    @InjectMocks
    private SeasonServiceImpl seasonService;

    @Test
    void createsSeasonIdentity() {
        CreateSeasonRequest request = createRequest();
        Season season = season();
        SeasonResponse expected = response();

        when(seasonMapper.toEntity(request)).thenReturn(season);
        when(seasonRepository.save(season)).thenReturn(season);
        when(seasonMapper.toResponse(season)).thenReturn(expected);

        assertThat(seasonService.create(request)).isSameAs(expected);
        verify(seasonRepository).save(season);
    }

    @Test
    void returnsPagedSeasons() {
        Pageable pageable = PageRequest.of(0, 20);
        Season season = season();
        SeasonResponse expected = response();
        when(seasonRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(season)));
        when(seasonMapper.toResponse(season)).thenReturn(expected);

        Page<SeasonResponse> actual = seasonService.findAll(pageable);

        assertThat(actual.getContent()).containsExactly(expected);
    }

    @Test
    void returnsSeasonById() {
        UUID id = UUID.randomUUID();
        Season season = season();
        SeasonResponse expected = response();

        when(seasonRepository.findById(id)).thenReturn(Optional.of(season));
        when(seasonMapper.toResponse(season)).thenReturn(expected);

        assertThat(seasonService.findById(id)).isSameAs(expected);
    }

    @Test
    void updatesExistingSeasonIdentity() {
        UUID id = UUID.randomUUID();
        Season season = season();
        UpdateSeasonRequest request = updateRequest();
        SeasonResponse expected = response();

        when(seasonRepository.findById(id)).thenReturn(Optional.of(season));
        when(seasonMapper.toResponse(season)).thenReturn(expected);

        assertThat(seasonService.update(id, request)).isSameAs(expected);
        verify(seasonMapper).updateEntity(season, request);
    }

    @Test
    void softDeletesExistingSeasonAndFreesLabel() {
        UUID id = UUID.randomUUID();
        Season season = season();
        // Soft-delete needs a persisted id to append to labelNormalized.
        try {
            var idField = season.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(season, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }

        when(seasonRepository.findById(id)).thenReturn(Optional.of(season));

        seasonService.softDelete(id);

        assertThat(season.isDeleted()).isTrue();
        assertThat(season.getLabelNormalized()).isEqualTo("2024/25#" + id);
    }

    @Test
    void rejectsUnknownSeason() {
        UUID id = UUID.randomUUID();
        when(seasonRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seasonService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    private CreateSeasonRequest createRequest() {
        return new CreateSeasonRequest("2024/25", LocalDate.of(2024, 7, 1), LocalDate.of(2025, 6, 30));
    }

    private UpdateSeasonRequest updateRequest() {
        return new UpdateSeasonRequest("2024/25", LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 31));
    }

    private Season season() {
        return new Season("2024/25", LocalDate.of(2024, 7, 1), LocalDate.of(2025, 6, 30));
    }

    private SeasonResponse response() {
        return new SeasonResponse(
                UUID.randomUUID(),
                "2024/25",
                LocalDate.of(2024, 7, 1),
                LocalDate.of(2025, 6, 30),
                null,
                null
        );
    }
}
