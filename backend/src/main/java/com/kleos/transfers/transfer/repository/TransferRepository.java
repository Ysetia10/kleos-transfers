package com.kleos.transfers.transfer.repository;

import com.kleos.transfers.transfer.entity.Transfer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for transfer records.
 */
public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    /**
     * Loads active transfers whose uniqueness keys match any of the given keys.
     */
    @Query("select t from Transfer t where t.uniquenessKey in :keys")
    List<Transfer> findAllByUniquenessKeyIn(@Param("keys") Collection<String> keys);
}
