package dev.bankheist.api.heist;

import dev.bankheist.api.common.ApiException;
import dev.bankheist.api.player.Player;
import dev.bankheist.api.player.PlayerRepository;
import dev.bankheist.api.quest.QuestService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HeistService {
    private final HeistRepository heists;
    private final PlayerRepository players;
    private final Clock clock;
    private final QuestService quests;

    public HeistService(HeistRepository heists, PlayerRepository players, Clock clock, QuestService quests) {
        this.heists = heists;
        this.players = players;
        this.clock = clock;
        this.quests = quests;
    }

    @Transactional
    public Submission submit(SubmitHeist request) {
        var existing = heists.findById(request.sessionId());
        if (existing.isPresent()) {
            HeistSession stored = existing.get();
            requireMatching(stored, request);
            Player player = players.findById(stored.getPlayerId()).orElseThrow();
            return new Submission(HeistView.from(stored, player.getUsername()), false);
        }
        // Serializes submissions for one player; progress updates later share this transaction and lock.
        Player player = players.findForUpdate(request.playerId())
                .orElseThrow(() -> ApiException.notFound("Player not found"));
        Instant completedAt = request.completedAt().truncatedTo(ChronoUnit.MICROS);
        Instant receivedAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
        int inserted = heists.insertIfAbsent(request.sessionId(), request.playerId(), request.level(),
                request.cashCollected(), request.diamondsCollected(), request.penaltiesIncurred(),
                request.finalScore(), request.completionTimeMs(), request.outcome().name(), completedAt, receivedAt);
        HeistSession stored = heists.findById(request.sessionId()).orElseThrow();
        requireMatching(stored, request);
        if (inserted == 1) quests.apply(player, stored);
        return new Submission(HeistView.from(stored, player.getUsername()), inserted == 1);
    }

    @Transactional(readOnly = true)
    public HeistView get(UUID sessionId) {
        HeistSession session = heists.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Heist not found"));
        Player player = players.findById(session.getPlayerId()).orElseThrow();
        return HeistView.from(session, player.getUsername());
    }

    private static void requireMatching(HeistSession stored, SubmitHeist request) {
        if (!stored.matches(request)) {
            throw ApiException.conflict("Session ID already belongs to a different result");
        }
    }

    public record Submission(HeistView heist, boolean created) {}
}
