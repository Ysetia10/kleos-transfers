package com.kleos.transfers.stats.service;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.club.repository.ClubRepository;
import com.kleos.transfers.player.dto.LatestClubView;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.player.repository.PlayerRepository;
import com.kleos.transfers.playerseason.repository.PlayerSeasonRepository;
import com.kleos.transfers.prediction.engine.EngineResult;
import com.kleos.transfers.prediction.engine.PredictionContext;
import com.kleos.transfers.prediction.engine.PredictionContextLoader;
import com.kleos.transfers.prediction.engine.PredictionEngine;
import com.kleos.transfers.prediction.entity.Prediction;
import com.kleos.transfers.prediction.repository.PredictionRepository;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.season.repository.SeasonRepository;
import com.kleos.transfers.stats.domain.LeagueCode;
import com.kleos.transfers.stats.dto.FitRouteResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ranks highest-fit player→club routes for the Trending surface.
 *
 * <p>Prefers stored simulator predictions, then fills gaps with a bounded batch of
 * hypothetical engine runs (top scorers × destination clubs).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FitRoutesService {

    private static final List<String> DESTINATION_NAMES = List.of(
            "arsenal",
            "manchester city",
            "liverpool",
            "chelsea",
            "real madrid",
            "barcelona",
            "bayern munich",
            "inter",
            "juventus",
            "psg"
    );

    private static final int MAX_HYPOTHETICAL_EVALUATIONS = 24;
    private static final long CACHE_TTL_SECONDS = 600;

    private final PredictionRepository predictionRepository;
    private final PlayerSeasonRepository playerSeasonRepository;
    private final PlayerRepository playerRepository;
    private final ClubRepository clubRepository;
    private final SeasonRepository seasonRepository;
    private final PredictionContextLoader contextLoader;
    private final PredictionEngine predictionEngine;

    private final Object cacheLock = new Object();
    private volatile Instant cacheExpiry = Instant.EPOCH;
    private volatile List<FitRouteResponse> cachedRoutes = List.of();
    private volatile int cachedLimit = -1;

    public List<FitRouteResponse> highestFitRoutes(int limit) {
        int capped = Math.min(Math.max(limit, 1), 20);
        Instant now = Instant.now();
        if (cachedLimit >= capped && now.isBefore(cacheExpiry) && !cachedRoutes.isEmpty()) {
            return cachedRoutes.stream().limit(capped).toList();
        }
        synchronized (cacheLock) {
            if (cachedLimit >= capped && Instant.now().isBefore(cacheExpiry) && !cachedRoutes.isEmpty()) {
                return cachedRoutes.stream().limit(capped).toList();
            }
            List<FitRouteResponse> routes = computeRoutes(Math.max(capped, 12));
            cachedRoutes = List.copyOf(routes);
            cachedLimit = routes.size();
            cacheExpiry = Instant.now().plusSeconds(CACHE_TTL_SECONDS);
            return routes.stream().limit(capped).toList();
        }
    }

    private List<FitRouteResponse> computeRoutes(int limit) {
        Map<String, FitRouteResponse> byPair = new LinkedHashMap<>();

        List<Prediction> stored = predictionRepository.findTopByCompatibility(PageRequest.of(0, 80));
        Map<UUID, LatestClubView> latestClubs = latestClubsFor(
                stored.stream().map(p -> p.getPlayer().getId()).distinct().toList()
        );
        for (Prediction prediction : stored) {
            LatestClubView from = latestClubs.get(prediction.getPlayer().getId());
            UUID fromClubId = from == null ? null : from.getClubId();
            // Skip same-club / non-move simulations — not useful as transfer routes.
            if (fromClubId != null && fromClubId.equals(prediction.getTargetClub().getId())) {
                continue;
            }
            FitRouteResponse route = fromPrediction(prediction, from);
            byPair.putIfAbsent(pairKey(route.playerId(), route.toClubId()), route);
        }

        if (byPair.size() < limit) {
            fillHypothetical(byPair, limit);
        }
        return ranked(byPair, limit);
    }

    private void fillHypothetical(Map<String, FitRouteResponse> byPair, int limit) {
        Optional<Season> seasonOpt = seasonRepository.findSeasonsWithPlayerDataOrderByStartDateDesc()
                .stream()
                .findFirst();
        if (seasonOpt.isEmpty()) {
            return;
        }
        Season season = seasonOpt.get();
        List<Club> destinations = clubRepository.findAllByNormalizedName(DESTINATION_NAMES);
        if (destinations.isEmpty()) {
            return;
        }

        List<UUID> candidatePlayerIds = new ArrayList<>();
        for (LeagueCode league : LeagueCode.values()) {
            List<Object[]> scorers = playerSeasonRepository.findSeasonGoalLeaders(
                    league.tournamentName(),
                    season.getId(),
                    2
            );
            for (Object[] row : scorers) {
                candidatePlayerIds.add((UUID) row[0]);
            }
        }
        if (candidatePlayerIds.isEmpty()) {
            // Fall back to any high-minute players in the latest season boards via all leagues' prior season
            Optional<Season> prior = seasonRepository.findFirstByStartDateLessThanOrderByStartDateDesc(
                    season.getStartDate());
            if (prior.isPresent()) {
                for (LeagueCode league : LeagueCode.values()) {
                    for (Object[] row : playerSeasonRepository.findSeasonGoalLeaders(
                            league.tournamentName(), prior.get().getId(), 2)) {
                        candidatePlayerIds.add((UUID) row[0]);
                    }
                }
            }
        }

        Map<UUID, LatestClubView> latestClubs = latestClubsFor(candidatePlayerIds);
        Map<UUID, Player> players = playerRepository.findAllById(candidatePlayerIds).stream()
                .collect(java.util.stream.Collectors.toMap(Player::getId, p -> p, (a, b) -> a));

        int evaluations = 0;
        for (UUID playerId : candidatePlayerIds) {
            if (byPair.size() >= limit || evaluations >= MAX_HYPOTHETICAL_EVALUATIONS) {
                break;
            }
            Player player = players.get(playerId);
            if (player == null) {
                continue;
            }
            LatestClubView from = latestClubs.get(playerId);
            UUID fromClubId = from == null ? null : from.getClubId();

            for (Club destination : destinations) {
                if (byPair.size() >= limit || evaluations >= MAX_HYPOTHETICAL_EVALUATIONS) {
                    break;
                }
                if (destination.getId().equals(fromClubId)) {
                    continue;
                }
                String key = pairKey(playerId, destination.getId());
                if (byPair.containsKey(key)) {
                    continue;
                }
                try {
                    PredictionContext context = contextLoader.load(playerId, destination.getId(), season.getId());
                    EngineResult result = predictionEngine.predict(context);
                    evaluations++;
                    byPair.put(key, new FitRouteResponse(
                            player.getId(),
                            player.getFullName(),
                            player.getPhotoUrl(),
                            fromClubId,
                            from == null ? null : from.getClubName(),
                            destination.getId(),
                            destination.getName(),
                            season.getId(),
                            season.getLabel(),
                            result.compatibilityScore(),
                            result.predictedMinutes(),
                            null,
                            "HYPOTHETICAL"
                    ));
                } catch (RuntimeException ignored) {
                    // Skip broken contexts (missing season history, etc.)
                }
            }
        }
    }

    private List<FitRouteResponse> ranked(Map<String, FitRouteResponse> byPair, int limit) {
        return byPair.values().stream()
                .sorted(Comparator
                        .comparing(FitRouteResponse::compatibilityScore, Comparator.reverseOrder())
                        .thenComparing(FitRouteResponse::predictedMinutes, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private FitRouteResponse fromPrediction(Prediction prediction, LatestClubView from) {
        return new FitRouteResponse(
                prediction.getPlayer().getId(),
                prediction.getPlayer().getFullName(),
                prediction.getPlayer().getPhotoUrl(),
                from == null ? null : from.getClubId(),
                from == null ? null : from.getClubName(),
                prediction.getTargetClub().getId(),
                prediction.getTargetClub().getName(),
                prediction.getSeason().getId(),
                prediction.getSeason().getLabel(),
                prediction.getCompatibilityScore(),
                prediction.getPredictedMinutes(),
                prediction.getId(),
                "STORED_PREDICTION"
        );
    }

    private Map<UUID, LatestClubView> latestClubsFor(List<UUID> playerIds) {
        if (playerIds.isEmpty()) {
            return Map.of();
        }
        Set<UUID> unique = new HashSet<>(playerIds);
        return playerSeasonRepository.findLatestClubsByPlayerIds(unique).stream()
                .collect(java.util.stream.Collectors.toMap(
                        LatestClubView::getPlayerId,
                        view -> view,
                        (a, b) -> a
                ));
    }

    private static String pairKey(UUID playerId, UUID clubId) {
        return Objects.toString(playerId, "none") + "->" + Objects.toString(clubId, "none");
    }
}
