package com.kleos.transfers.manager.service;

import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.manager.dto.CreateManagerRequest;
import com.kleos.transfers.manager.dto.ManagerResponse;
import com.kleos.transfers.manager.dto.UpdateManagerRequest;
import com.kleos.transfers.manager.entity.Manager;
import com.kleos.transfers.manager.mapper.ManagerMapper;
import com.kleos.transfers.manager.repository.ManagerRepository;
import java.util.UUID;
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

    @Override
    @Transactional
    public ManagerResponse create(CreateManagerRequest request) {
        Manager manager = managerMapper.toEntity(request);
        return managerMapper.toResponse(managerRepository.save(manager));
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
}
