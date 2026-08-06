package com.kleos.transfers.manager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.manager.dto.CreateManagerRequest;
import com.kleos.transfers.manager.dto.ManagerResponse;
import com.kleos.transfers.manager.dto.UpdateManagerRequest;
import com.kleos.transfers.manager.entity.Manager;
import com.kleos.transfers.manager.mapper.ManagerMapper;
import com.kleos.transfers.manager.repository.ManagerRepository;
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
class ManagerServiceImplTest {

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private ManagerMapper managerMapper;

    @Mock
    private BulkImporter bulkImporter;

    @InjectMocks
    private ManagerServiceImpl managerService;

    @Test
    void createsManagerIdentity() {
        CreateManagerRequest request = createRequest();
        Manager manager = manager();
        ManagerResponse expected = response();

        when(managerMapper.toEntity(request)).thenReturn(manager);
        when(managerRepository.save(manager)).thenReturn(manager);
        when(managerMapper.toResponse(manager)).thenReturn(expected);

        assertThat(managerService.create(request)).isSameAs(expected);
        verify(managerRepository).save(manager);
    }

    @Test
    void returnsPagedManagers() {
        Pageable pageable = PageRequest.of(0, 20);
        Manager manager = manager();
        ManagerResponse expected = response();
        when(managerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(manager)));
        when(managerMapper.toResponse(manager)).thenReturn(expected);

        Page<ManagerResponse> actual = managerService.findAll(pageable);

        assertThat(actual.getContent()).containsExactly(expected);
    }

    @Test
    void returnsManagerById() {
        UUID id = UUID.randomUUID();
        Manager manager = manager();
        ManagerResponse expected = response();

        when(managerRepository.findById(id)).thenReturn(Optional.of(manager));
        when(managerMapper.toResponse(manager)).thenReturn(expected);

        assertThat(managerService.findById(id)).isSameAs(expected);
    }

    @Test
    void updatesExistingManagerIdentity() {
        UUID id = UUID.randomUUID();
        Manager manager = manager();
        UpdateManagerRequest request = updateRequest();
        ManagerResponse expected = response();

        when(managerRepository.findById(id)).thenReturn(Optional.of(manager));
        when(managerMapper.toResponse(manager)).thenReturn(expected);

        assertThat(managerService.update(id, request)).isSameAs(expected);
        verify(managerMapper).updateEntity(manager, request);
    }

    @Test
    void softDeletesExistingManager() {
        UUID id = UUID.randomUUID();
        Manager manager = manager();
        when(managerRepository.findById(id)).thenReturn(Optional.of(manager));

        managerService.softDelete(id);

        assertThat(manager.isDeleted()).isTrue();
        assertThat(manager.getDeletedAt()).isNotNull();
    }

    @Test
    void rejectsUnknownManager() {
        UUID id = UUID.randomUUID();
        when(managerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> managerService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    private CreateManagerRequest createRequest() {
        return new CreateManagerRequest("Mikel Arteta", LocalDate.of(1982, 3, 26), "ESP");
    }

    private UpdateManagerRequest updateRequest() {
        return new UpdateManagerRequest("Mikel Arteta Amatriain", LocalDate.of(1982, 3, 26), "ESP");
    }

    private Manager manager() {
        return new Manager("Mikel Arteta", LocalDate.of(1982, 3, 26), "ESP");
    }

    private ManagerResponse response() {
        return new ManagerResponse(
                UUID.randomUUID(),
                "Mikel Arteta",
                LocalDate.of(1982, 3, 26),
                "ESP",
                null,
                null
        );
    }
}
