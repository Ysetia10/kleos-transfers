package com.kleos.transfers.contract.repository;

import com.kleos.transfers.contract.entity.Contract;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for contract records.
 */
public interface ContractRepository extends JpaRepository<Contract, UUID> {

    /**
     * Loads active contracts whose uniqueness keys match any of the given keys.
     */
    @Query("select c from Contract c where c.uniquenessKey in :keys")
    List<Contract> findAllByUniquenessKeyIn(@Param("keys") Collection<String> keys);

    List<Contract> findByPlayerIdOrderByEndDateDesc(UUID playerId);
}
