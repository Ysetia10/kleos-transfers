package com.kleos.transfers.player.mapper;

import com.kleos.transfers.common.dto.UpdateIdentityMediaRequest;
import com.kleos.transfers.domain.DateOfBirthPrecision;
import com.kleos.transfers.player.dto.CreatePlayerRequest;
import com.kleos.transfers.player.dto.LatestClubView;
import com.kleos.transfers.player.dto.PlayerResponse;
import com.kleos.transfers.player.dto.UpdatePlayerRequest;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.transfer.dto.TransferMoveSummary;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Maps player identity persistence models to and from API contracts.
 */
@Component
public class PlayerMapper {

    public Player toEntity(CreatePlayerRequest request) {
        return new Player(
                request.fullName(),
                request.dateOfBirth(),
                precisionOrDay(request.dateOfBirthPrecision()),
                request.nationality(),
                request.heightCm(),
                request.preferredFoot(),
                request.primaryPosition(),
                request.fbrefId()
        );
    }

    public void updateEntity(Player player, UpdatePlayerRequest request) {
        player.update(
                request.fullName(),
                request.dateOfBirth(),
                request.dateOfBirthPrecision() == null
                        ? player.getDateOfBirthPrecision()
                        : request.dateOfBirthPrecision(),
                request.nationality(),
                request.heightCm(),
                request.preferredFoot(),
                request.primaryPosition(),
                request.fbrefId()
        );
    }

    public void updateMedia(Player player, UpdateIdentityMediaRequest request) {
        player.updateMedia(
                request.imageUrl(),
                request.attribution(),
                request.license(),
                request.source()
        );
    }

    public PlayerResponse toResponse(Player player, LatestClubView latestClub) {
        return toResponse(player, latestClub, null);
    }

    public PlayerResponse toResponse(
            Player player,
            LatestClubView latestClub,
            TransferMoveSummary latestTransfer
    ) {
        UUID clubId;
        String clubName;
        String seasonLabel;
        if (latestTransfer != null && latestTransfer.toClubId() != null) {
            clubId = latestTransfer.toClubId();
            clubName = latestTransfer.toClubName();
            seasonLabel = latestTransfer.seasonLabel();
        } else {
            clubId = latestClub == null ? null : latestClub.getClubId();
            clubName = latestClub == null ? null : latestClub.getClubName();
            seasonLabel = latestClub == null ? null : latestClub.getSeasonLabel();
        }
        return new PlayerResponse(
                player.getId(),
                player.getFullName(),
                player.getDateOfBirth(),
                player.getDateOfBirthPrecision(),
                ageYears(player.getDateOfBirth()),
                player.getNationality(),
                player.getHeightCm(),
                player.getPreferredFoot(),
                player.getPrimaryPosition(),
                player.getFbrefId(),
                player.getPhotoUrl(),
                player.getPhotoAttribution(),
                player.getPhotoLicense(),
                player.getPhotoSource(),
                clubId,
                clubName,
                seasonLabel,
                latestTransfer,
                player.getCreatedAt(),
                player.getUpdatedAt()
        );
    }

    private static Integer ageYears(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return null;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    private static DateOfBirthPrecision precisionOrDay(DateOfBirthPrecision precision) {
        return precision == null ? DateOfBirthPrecision.DAY : precision;
    }
}
