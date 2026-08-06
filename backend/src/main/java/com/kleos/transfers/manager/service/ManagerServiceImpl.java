package com.kleos.transfers.manager.service;

import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.common.bulk.BulkImportSpec;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.bulk.NaturalKeys;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.manager.dto.CreateManagerRequest;
import com.kleos.transfers.manager.dto.ManagerResponse;
import com.kleos.transfers.manager.dto.UpdateManagerRequest;
import com.kleos.transfers.manager.entity.Manager;
import com.kleos.transfers.manager.mapper.ManagerMapper;
import com.kleos.transfers.manager.repository.ManagerRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for manager identity use cases.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerServiceImpl implements ManagerService {

    private final ManagerRepository managerRepository;
    private final ManagerMapper managerMapper;
    private final BulkImporter bulkImporter;

    @Override
    @Transactional
    public ManagerResponse create(CreateManagerRequest request) {
        Manager manager = managerMapper.toEntity(request);
        return managerMapper.toResponse(managerRepository.save(manager));
    }

    @Override
    @Transactional
    public BulkImportResponse<ManagerResponse> createAll(List<CreateManagerRequest> requests) {
        return bulkImporter.importAll(requests, new ManagerBulkSpec());
    }

    @Override
    public Page<ManagerResponse> findAll(Pageable pageable) {
        return managerRepository.findAll(pageable).map(managerMapper::toResponse);
    }

    @Override
    public ManagerResponse findById(UUID id) {
        return managerMapper.toResponse(findManager(id));
    }

    @Override
    @Transactional
    public ManagerResponse update(UUID id, UpdateManagerRequest request) {
        Manager manager = findManager(id);
        managerMapper.updateEntity(manager, request);
        return managerMapper.toResponse(manager);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        Manager manager = findManager(id);
        manager.softDelete();
    }

    private Manager findManager(UUID id) {
        return managerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Manager", id));
    }

    /**
     * A manager is considered a duplicate when name, date of birth, and nationality all match.
     */
    private final class ManagerBulkSpec implements BulkImportSpec<CreateManagerRequest, ManagerResponse> {

        @Override
        public String naturalKey(CreateManagerRequest request) {
            return NaturalKeys.of(request.fullName(), request.dateOfBirth(), request.nationality());
        }

        @Override
        public String reference(CreateManagerRequest request) {
            return String.valueOf(request.fullName());
        }

        @Override
        public Set<String> findExistingKeys(List<CreateManagerRequest> requests) {
            Set<String> names = requests.stream()
                    .map(request -> request.fullName().trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            return managerRepository.findAllByNormalizedName(names).stream()
                    .map(manager -> NaturalKeys.of(
                            manager.getFullName(), manager.getDateOfBirth(), manager.getNationality()))
                    .collect(Collectors.toSet());
        }

        @Override
        public List<ManagerResponse> persist(List<CreateManagerRequest> accepted) {
            List<Manager> managers = accepted.stream().map(managerMapper::toEntity).toList();
            return managerRepository.saveAll(managers).stream().map(managerMapper::toResponse).toList();
        }
    }
}
