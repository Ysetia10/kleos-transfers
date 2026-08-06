package com.kleos.transfers.club.service;

import com.kleos.transfers.club.dto.ClubResponse;
import com.kleos.transfers.club.dto.CreateClubRequest;
import com.kleos.transfers.club.dto.UpdateClubRequest;
import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.club.mapper.ClubMapper;
import com.kleos.transfers.club.repository.ClubRepository;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for club identity use cases.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubServiceImpl implements ClubService {

    private final ClubRepository clubRepository;
    private final ClubMapper clubMapper;

    @Override
    @Transactional
    public ClubResponse create(CreateClubRequest request) {
        Club club = clubMapper.toEntity(request);
        return clubMapper.toResponse(clubRepository.save(club));
    }

    @Override
    public Page<ClubResponse> findAll(Pageable pageable) {
        return clubRepository.findAll(pageable).map(clubMapper::toResponse);
    }

    @Override
    public ClubResponse findById(UUID id) {
        return clubMapper.toResponse(findClub(id));
    }

    @Override
    @Transactional
    public ClubResponse update(UUID id, UpdateClubRequest request) {
        Club club = findClub(id);
        clubMapper.updateEntity(club, request);
        return clubMapper.toResponse(club);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        Club club = findClub(id);
        club.softDelete();
    }

    private Club findClub(UUID id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Club", id));
    }
}
