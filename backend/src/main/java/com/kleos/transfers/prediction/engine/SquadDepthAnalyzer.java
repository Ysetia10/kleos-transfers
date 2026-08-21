package com.kleos.transfers.prediction.engine;

import com.kleos.transfers.domain.Position;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Builds the depth chart a player walks into: who else plays his role next season, how established
 * they are, and whether the window vacated a slot he is being signed to fill.
 *
 * <p>The pool is the prior-season roster minus players leaving in the target window plus the other
 * arrivals (carrying the minutes they logged at their previous club). Rivals are weighted by role
 * overlap and by how locked-in they look, so a 3,000-minute incumbent blocks the same role far more
 * than a squad player who barely featured — and a departing starter opens the door instead.
 */
final class SquadDepthAnalyzer {

    /** Minutes at which a squad member reads as a fully locked-in starter. */
    static final int LOCKED_STARTER_MINUTES = 2_600;
    /** Minutes a departing player must have logged for his exit to count as a vacated starting slot. */
    static final int VACATED_STARTER_MINUTES = 1_500;
    /** Weighted vacancy above which the club is clearly replacing a first-choice player. */
    static final double STARTER_SLOT_VACATED = 0.55;
    /** Share of the pool that must carry finer-than-line roles before exact slots are trusted. */
    static final double PRECISE_ROLE_SHARE = 0.35;

    private SquadDepthAnalyzer() {
    }

    /**
     * One squad member measured against the subject's role.
     *
     * @param overlap  share of the subject's role this player can cover (0–1)
     * @param strength how established the player looks, from minutes (0–1)
     * @param blocking claim on a slot the subject wants, counted only where the rival ranks ahead
     */
    record Contender(String name, Position role, int minutes, double overlap, double strength, double blocking) {

        /**
         * Claim on the role. Overlap is squared because a part-time cover option is usually busy in
         * his own role: a centre-back who can fill in at right-back rarely takes those minutes.
         */
        double load() {
            return overlap * overlap * strength;
        }
    }

    /**
     * @param preciseRoles false when the pool still carries GK/DF/MF/FW-derived codes, so competition
     *                     is judged across a whole line rather than exact flanks
     * @param slots        starting slots available at the subject's role
     * @param contested    total weighted rival load on those slots
     * @param blocked      weighted slots held by rivals more established than the subject
     * @param vacated      weighted starting load leaving the club in this window
     */
    record Assessment(
            Position role,
            boolean preciseRoles,
            double slots,
            double contested,
            double blocked,
            double vacated,
            List<Contender> rivals,
            List<Contender> departures
    ) {

        /** Starting slots left once more established rivals are seated. */
        double openness() {
            return slots - blocked;
        }

        /** Rivals per starting slot, including those the subject outranks. */
        double crowding() {
            return contested / slots;
        }

        boolean starterSlotVacated() {
            return vacated >= STARTER_SLOT_VACATED;
        }

        /**
         * Thin remaining depth at the role — no starter-level rival left — even when no
         * transfer-tagged departure was recorded.
         */
        boolean openDepthForWalkIn() {
            int topRivalMinutes = rivals.stream()
                    .mapToInt(Contender::minutes)
                    .max()
                    .orElse(0);
            return topRivalMinutes < VACATED_STARTER_MINUTES;
        }

        Optional<Contender> topBlocker() {
            return rivals.stream()
                    .filter(rival -> rival.blocking() > 0.1)
                    .max(Comparator.comparingDouble(Contender::blocking));
        }

        Optional<Contender> topDeparture() {
            return departures.stream().max(Comparator.comparingDouble(Contender::load));
        }
    }

    /**
     * Whether the post-transfer squad carries roles fine-grained enough to judge an exact slot.
     */
    static boolean hasPreciseRoles(PredictionContext context, Position role) {
        return preciseRoles(role, competingSquad(context));
    }

    static Assessment analyze(PredictionContext context, Position role, int baselineMinutes) {
        List<PlayerSeason> pool = competingSquad(context);
        Set<UUID> poolIds = new HashSet<>();
        for (PlayerSeason row : pool) {
            poolIds.add(row.getPlayer().getId());
        }

        boolean precise = preciseRoles(role, pool);
        double slots = precise ? RoleProfiles.exactSlots(role) : RoleProfiles.lineSlots(role);

        List<Contender> rivals = new ArrayList<>();
        double contested = 0;
        double blocked = 0;
        for (PlayerSeason row : pool) {
            Contender rival = contender(row, role, precise, baselineMinutes);
            if (rival.load() <= 0) {
                continue;
            }
            rivals.add(rival);
            contested += rival.load();
            blocked += rival.blocking();
        }

        // Explicit window departures + prior-roster starters already removed from the competing pool
        // (transfer-tagged outs, including null fromClub caught by the loader).
        Map<UUID, Contender> departuresById = new LinkedHashMap<>();
        for (PlayerSeason row : context.departingSquadMembers()) {
            if (row.getPlayer().getId().equals(context.player().getId())) {
                continue;
            }
            Contender leaver = contender(row, role, precise, baselineMinutes);
            if (leaver.minutes() < VACATED_STARTER_MINUTES || leaver.load() <= 0) {
                continue;
            }
            departuresById.put(row.getPlayer().getId(), leaver);
        }
        for (PlayerSeason row : context.targetClubSquad()) {
            UUID playerId = row.getPlayer().getId();
            if (playerId.equals(context.player().getId()) || poolIds.contains(playerId)) {
                continue;
            }
            Contender leaver = contender(row, role, precise, baselineMinutes);
            if (leaver.minutes() < VACATED_STARTER_MINUTES || leaver.load() <= 0) {
                continue;
            }
            departuresById.putIfAbsent(playerId, leaver);
        }

        List<Contender> departures = new ArrayList<>(departuresById.values());
        double vacated = departures.stream().mapToDouble(Contender::load).sum();

        return new Assessment(role, precise, slots, contested, blocked, vacated, rivals, departures);
    }

    /**
     * Prior-season roster after the target window: departures removed, other arrivals added with the
     * minutes they logged at their previous club.
     */
    private static List<PlayerSeason> competingSquad(PredictionContext context) {
        UUID subjectId = context.player().getId();
        Set<UUID> leavingIds = new HashSet<>();
        for (PlayerSeason row : context.departingSquadMembers()) {
            leavingIds.add(row.getPlayer().getId());
        }

        List<PlayerSeason> pool = new ArrayList<>();
        for (PlayerSeason row : context.targetClubSquad()) {
            UUID playerId = row.getPlayer().getId();
            if (playerId.equals(subjectId) || leavingIds.contains(playerId)) {
                continue;
            }
            pool.add(row);
        }
        for (PlayerSeason row : context.arrivingSquadMembers()) {
            if (!row.getPlayer().getId().equals(subjectId)) {
                pool.add(row);
            }
        }
        return pool;
    }

    private static Contender contender(PlayerSeason row, Position role, boolean precise, int baselineMinutes) {
        Position rivalRole = row.getPrimaryPosition();
        double overlap = rivalRole == null
                ? 0
                : precise ? RoleProfiles.exactOverlap(role, rivalRole) : RoleProfiles.lineOverlap(role, rivalRole);
        int minutes = row.getMinutesPlayed() == null ? 0 : row.getMinutesPlayed();
        double strength = Math.min(1.0, Math.max(0.10, minutes / (double) LOCKED_STARTER_MINUTES));
        double blocking = overlap * overlap * strength * seniority(minutes, baselineMinutes);
        return new Contender(row.getPlayer().getFullName(), rivalRole, minutes, overlap, strength, blocking);
    }

    /**
     * How firmly a rival sits ahead of the subject: full credit once he is 400+ minutes clear, none
     * once the subject is 400+ minutes clear, ramped in between so the ranking is not knife-edge.
     */
    private static double seniority(int rivalMinutes, int baselineMinutes) {
        double edge = rivalMinutes - baselineMinutes;
        return Math.min(1.0, Math.max(0.0, (edge + 400.0) / 800.0));
    }

    /**
     * Exact slots are only trustworthy when the squad's roles came from match-level data. A squad
     * still on the GK/DF/MF/FW feed carries no flank codes at all, so a run of full-backs and
     * wingers in the pool is the signal that roles have been enriched. Goalkeeper is unambiguous
     * either way.
     */
    private static boolean preciseRoles(Position role, List<PlayerSeason> pool) {
        if (role == Position.GK) {
            return true;
        }
        List<Position> outfield = pool.stream()
                .map(PlayerSeason::getPrimaryPosition)
                .filter(position -> position != null && position != Position.GK)
                .toList();
        if (outfield.isEmpty()) {
            return !RoleProfiles.isCoarse(role);
        }
        long precise = outfield.stream().filter(position -> !RoleProfiles.isCoarse(position)).count();
        return precise >= Math.max(2, Math.ceil(outfield.size() * PRECISE_ROLE_SHARE));
    }
}
