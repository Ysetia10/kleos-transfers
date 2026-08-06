package com.kleos.transfers.tournament.mapper;

import com.kleos.transfers.tournament.dto.CreateTournamentRequest;
import com.kleos.transfers.tournament.dto.TournamentResponse;
import com.kleos.transfers.tournament.dto.UpdateTournamentRequest;
import com.kleos.transfers.tournament.entity.Tournament;
import org.springframework.stereotype.Component;

/**
 * Maps tournament identity persistence models to and from API contracts.
 */
@Component
public class TournamentMapper {

    public Tournament toEntity(CreateTournamentRequest request) {
        return new Tournament(
                request.name(),
                request.shortName(),
                request.confederation(),
                request.type(),
                request.countryCode()
        );
    }

    public void updateEntity(Tournament tournament, UpdateTournamentRequest request) {
        tournament.update(
                request.name(),
                request.shortName(),
                request.confederation(),
                request.type(),
                request.countryCode()
        );
    }

    public TournamentResponse toResponse(Tournament tournament) {
        return new TournamentResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getShortName(),
                tournament.getConfederation(),
                tournament.getType(),
                tournament.getCountryCode(),
                tournament.getCreatedAt(),
                tournament.getUpdatedAt()
        );
    }
}
