package com.kleos.transfers.season.mapper;

import com.kleos.transfers.season.dto.CreateSeasonRequest;
import com.kleos.transfers.season.dto.SeasonResponse;
import com.kleos.transfers.season.dto.UpdateSeasonRequest;
import com.kleos.transfers.season.entity.Season;
import org.springframework.stereotype.Component;

/**
 * Maps season identity persistence models to and from API contracts.
 */
@Component
public class SeasonMapper {

    public Season toEntity(CreateSeasonRequest request) {
        return new Season(request.label(), request.startDate(), request.endDate());
    }

    public void updateEntity(Season season, UpdateSeasonRequest request) {
        season.update(request.label(), request.startDate(), request.endDate());
    }

    public SeasonResponse toResponse(Season season) {
        return new SeasonResponse(
                season.getId(),
                season.getLabel(),
                season.getStartDate(),
                season.getEndDate(),
                season.getCreatedAt(),
                season.getUpdatedAt()
        );
    }
}
