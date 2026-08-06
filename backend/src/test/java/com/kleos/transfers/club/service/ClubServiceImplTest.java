package com.kleos.transfers.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kleos.transfers.club.dto.ClubResponse;
import com.kleos.transfers.club.dto.CreateClubRequest;
import com.kleos.transfers.club.dto.UpdateClubRequest;
import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.club.mapper.ClubMapper;
import com.kleos.transfers.club.repository.ClubRepository;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.exception.ConflictException;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
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
class ClubServiceImplTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMapper clubMapper;

    @Mock
    private BulkImporter bulkImporter;

    @InjectMocks
    private ClubServiceImpl clubService;

    @Test
    void createsClubIdentity() {
        CreateClubRequest request = createRequest();
        Club club = club();
        ClubResponse expected = response();

        when(clubRepository.existsByNameNormalizedAndCountryCode("fc barcelona", "ESP")).thenReturn(false);
        when(clubMapper.toEntity(request)).thenReturn(club);
        when(clubRepository.save(club)).thenReturn(club);
        when(clubMapper.toResponse(club)).thenReturn(expected);

        assertThat(clubService.create(request)).isSameAs(expected);
        verify(clubRepository).save(club);
    }

    @Test
    void rejectsDuplicateClubNaturalKeyOnCreate() {
        CreateClubRequest request = createRequest();
        when(clubRepository.existsByNameNormalizedAndCountryCode("fc barcelona", "ESP")).thenReturn(true);

        assertThatThrownBy(() -> clubService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void returnsPagedClubs() {
        Pageable pageable = PageRequest.of(0, 20);
        Club club = club();
        ClubResponse expected = response();
        when(clubRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(club)));
        when(clubMapper.toResponse(club)).thenReturn(expected);

        Page<ClubResponse> actual = clubService.findAll(pageable);

        assertThat(actual.getContent()).containsExactly(expected);
    }

    @Test
    void returnsClubById() {
        UUID id = UUID.randomUUID();
        Club club = club();
        ClubResponse expected = response();

        when(clubRepository.findById(id)).thenReturn(Optional.of(club));
        when(clubMapper.toResponse(club)).thenReturn(expected);

        assertThat(clubService.findById(id)).isSameAs(expected);
    }

    @Test
    void updatesExistingClubIdentity() {
        UUID id = UUID.randomUUID();
        Club club = club();
        UpdateClubRequest request = updateRequest();
        ClubResponse expected = response();

        when(clubRepository.findById(id)).thenReturn(Optional.of(club));
        when(clubRepository.existsByNameNormalizedAndCountryCodeAndIdNot("fc barcelona", "ESP", id))
                .thenReturn(false);
        when(clubMapper.toResponse(club)).thenReturn(expected);

        assertThat(clubService.update(id, request)).isSameAs(expected);
        verify(clubMapper).updateEntity(club, request);
    }

    @Test
    void softDeletesExistingClub() {
        UUID id = UUID.randomUUID();
        Club club = club();
        when(clubRepository.findById(id)).thenReturn(Optional.of(club));

        clubService.softDelete(id);

        assertThat(club.isDeleted()).isTrue();
        assertThat(club.getDeletedAt()).isNotNull();
    }

    @Test
    void rejectsUnknownClub() {
        UUID id = UUID.randomUUID();
        when(clubRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    private CreateClubRequest createRequest() {
        return new CreateClubRequest("FC Barcelona", "Barcelona", "ESP", 1899, null);
    }

    private UpdateClubRequest updateRequest() {
        return new UpdateClubRequest("FC Barcelona", "Barça", "ESP", 1899, null);
    }

    private Club club() {
        return new Club("FC Barcelona", "Barcelona", "ESP", 1899);
    }

    private ClubResponse response() {
        return new ClubResponse(
                UUID.randomUUID(),
                "FC Barcelona",
                "Barcelona",
                "ESP",
                1899,
                null,
                null,
                null
        );
    }
}
