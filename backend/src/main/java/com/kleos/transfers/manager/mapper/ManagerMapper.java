package com.kleos.transfers.manager.mapper;

import com.kleos.transfers.manager.dto.CreateManagerRequest;
import com.kleos.transfers.manager.dto.ManagerResponse;
import com.kleos.transfers.manager.dto.UpdateManagerRequest;
import com.kleos.transfers.manager.entity.Manager;
import org.springframework.stereotype.Component;

/**
 * Maps manager identity persistence models to and from API contracts.
 */
@Component
public class ManagerMapper {

    public Manager toEntity(CreateManagerRequest request) {
        return new Manager(
                request.fullName(),
                request.dateOfBirth(),
                request.nationality()
        );
    }

    public void updateEntity(Manager manager, UpdateManagerRequest request) {
        manager.update(
                request.fullName(),
                request.dateOfBirth(),
                request.nationality()
        );
    }

    public ManagerResponse toResponse(Manager manager) {
        return new ManagerResponse(
                manager.getId(),
                manager.getFullName(),
                manager.getDateOfBirth(),
                manager.getNationality(),
                manager.getCreatedAt(),
                manager.getUpdatedAt()
        );
    }
}
