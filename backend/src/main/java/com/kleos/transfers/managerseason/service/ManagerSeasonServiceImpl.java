package com.kleos.transfers.managerseason.service;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.club.repository.ClubRepository;
import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.common.bulk.BulkImportSpec;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.bulk.NaturalKeys;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.manager.entity.Manager;
import com.kleos.transfers.manager.repository.ManagerRepository;
import com.kleos.transfers.managerseason.dto.CreateManagerSeasonRequest;
import com.kleos.transfers.managerseason.dto.ManagerSeasonResponse;
import com.kleos.transfers.managerseason.dto.UpdateManagerSeasonRequest;
import com.kleos.transfers.managerseason.entity.ManagerSeason;
import com.kleos.transfers.managerseason.mapper.ManagerSeasonMapper;
import com.kleos.transfers.managerseason.repository.ManagerSeasonRepository;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.season.repository.SeasonRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for manager-season appointment use cases.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerSeasonServiceImpl implements ManagerSeasonService {

    private final ManagerSeasonRepository managerSeasonRepository;
    private final ManagerRepository managerRepository;
    private final ClubRepository clubRepository;
    private final SeasonRepository seasonRepository;
    private final ManagerSeasonMapper managerSeasonMapper;
    private final BulkImporter bulkImporter;

    @Override
    @Transactional
    public ManagerSeasonResponse create(CreateManagerSeasonRequest request) {
        Manager manager = requireManager(request.managerId());
        Club club = requireClub(request.clubId());
        Season season = requireSeason(request.seasonId());
        ManagerSeason managerSeason = managerSeasonMapper.toEntity(
                manager,
                club,
                season,
                request.tacticalSystem(),
                request.tempo(),
                request.youthMinutesPct()
        );
        return managerSeasonMapper.toResponse(managerSeasonRepository.save(managerSeason));
    }

    @Override
    @Transactional
    public BulkImportResponse<ManagerSeasonResponse> createAll(List<CreateManagerSeasonRequest> requests) {
        return bulkImporter.importAll(requests, new ManagerSeasonBulkSpec());
    }

    @Override
    public Page<ManagerSeasonResponse> findAll(Pageable pageable) {
        return managerSeasonRepository.findAll(pageable).map(managerSeasonMapper::toResponse);
    }

    @Override
    public ManagerSeasonResponse findById(UUID id) {
        return managerSeasonMapper.toResponse(findManagerSeason(id));
    }

    @Override
    @Transactional
    public ManagerSeasonResponse update(UUID id, UpdateManagerSeasonRequest request) {
        ManagerSeason managerSeason = findManagerSeason(id);
        Manager manager = requireManager(request.managerId());
        Club club = requireClub(request.clubId());
        Season season = requireSeason(request.seasonId());
        managerSeason.reassign(manager, club, season);
        managerSeason.updateTactics(request.tacticalSystem(), request.tempo(), request.youthMinutesPct());
        return managerSeasonMapper.toResponse(managerSeason);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        findManagerSeason(id).softDelete();
    }

    private ManagerSeason findManagerSeason(UUID id) {
        return managerSeasonRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ManagerSeason", id));
    }

    private Manager requireManager(UUID id) {
        return managerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Manager", id));
    }

    private Club requireClub(UUID id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Club", id));
    }

    private Season requireSeason(UUID id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Season", id));
    }

    private final class ManagerSeasonBulkSpec
            implements BulkImportSpec<CreateManagerSeasonRequest, ManagerSeasonResponse> {

        @Override
        public String naturalKey(CreateManagerSeasonRequest request) {
            return NaturalKeys.of(request.managerId(), request.clubId(), request.seasonId());
        }

        @Override
        public String reference(CreateManagerSeasonRequest request) {
            return request.managerId() + " / " + request.clubId() + " / " + request.seasonId();
        }

        @Override
        public Set<String> findExistingKeys(List<CreateManagerSeasonRequest> requests) {
            Set<String> keys = requests.stream()
                    .map(request -> request.managerId() + ":" + request.clubId() + ":" + request.seasonId())
                    .collect(Collectors.toSet());
            return managerSeasonRepository.findAllByUniquenessKeyIn(keys).stream()
                    .map(managerSeason -> NaturalKeys.of(
                            managerSeason.getManager().getId(),
                            managerSeason.getClub().getId(),
                            managerSeason.getSeason().getId()))
                    .collect(Collectors.toSet());
        }

        @Override
        public List<ManagerSeasonResponse> persist(List<CreateManagerSeasonRequest> accepted) {
            Set<UUID> managerIds = new HashSet<>();
            Set<UUID> clubIds = new HashSet<>();
            Set<UUID> seasonIds = new HashSet<>();
            for (CreateManagerSeasonRequest request : accepted) {
                managerIds.add(request.managerId());
                clubIds.add(request.clubId());
                seasonIds.add(request.seasonId());
            }

            Map<UUID, Manager> managers = managerRepository.findAllById(managerIds).stream()
                    .collect(Collectors.toMap(Manager::getId, manager -> manager));
            Map<UUID, Club> clubs = clubRepository.findAllById(clubIds).stream()
                    .collect(Collectors.toMap(Club::getId, club -> club));
            Map<UUID, Season> seasons = seasonRepository.findAllById(seasonIds).stream()
                    .collect(Collectors.toMap(Season::getId, season -> season));

            List<ManagerSeason> entities = accepted.stream()
                    .map(request -> managerSeasonMapper.toEntity(
                            requirePresent(managers, request.managerId(), "Manager"),
                            requirePresent(clubs, request.clubId(), "Club"),
                            requirePresent(seasons, request.seasonId(), "Season"),
                            request.tacticalSystem(),
                            request.tempo(),
                            request.youthMinutesPct()))
                    .toList();

            return managerSeasonRepository.saveAll(entities).stream()
                    .map(managerSeasonMapper::toResponse)
                    .toList();
        }

        private <T> T requirePresent(Map<UUID, T> indexed, UUID id, String resource) {
            T value = indexed.get(id);
            if (value == null) {
                throw ResourceNotFoundException.of(resource, id);
            }
            return value;
        }
    }
}
