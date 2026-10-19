package dev.bankheist.api.heist;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import dev.bankheist.api.common.ApiException;
import dev.bankheist.api.player.Player;
import dev.bankheist.api.player.PlayerRepository;
import dev.bankheist.api.quest.QuestService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class HeistServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-18T15:00:00Z");
    private final HeistRepository heists = mock(HeistRepository.class);
    private final PlayerRepository players = mock(PlayerRepository.class);
    private final QuestService quests = mock(QuestService.class);
    private final HeistService service = new HeistService(heists, players, Clock.fixed(NOW, ZoneOffset.UTC), quests);
    private final UUID playerId = UUID.randomUUID();
    private final SubmitHeist request = new SubmitHeist(UUID.randomUUID(), playerId, 1,
            100L, 0, 150L, -50L, 1_500L, Outcome.LOST, NOW.minusSeconds(1));

    @BeforeEach
    void playerExists() {
        when(players.findForUpdate(playerId))
                .thenReturn(Optional.of(new Player(playerId, "Player_1", "player_1", NOW)));
        when(players.findById(playerId))
                .thenReturn(Optional.of(new Player(playerId, "Player_1", "player_1", NOW)));
        when(heists.findById(request.sessionId())).thenReturn(Optional.of(new HeistSession(request, NOW)));
    }

    @Test
    void persistsClientResultIncludingNegativeFinalScore() {
        whenInsertReturns(1);
        when(heists.findById(request.sessionId()))
                .thenReturn(Optional.empty(), Optional.of(new HeistSession(request, NOW)));

        HeistService.Submission result = service.submit(request);

        assertTrue(result.created());
        assertEquals(-50, result.heist().finalScore());
        assertEquals(100, result.heist().cashCollected());
        assertEquals(150, result.heist().penaltiesIncurred());
        verify(heists).insertIfAbsent(request.sessionId(), playerId, 1, 100, 0, 150,
                -50, 1_500, "LOST", request.completedAt(), NOW);
        verify(quests).apply(any(Player.class), any(HeistSession.class));
    }

    @Test
    void exactRetryReturnsExistingResult() {
        whenInsertReturns(0);
        assertFalse(service.submit(request).created());
        verifyNoInteractions(quests);
    }

    @Test
    void reusedSessionWithChangedResultIsConflict() {
        SubmitHeist changed = new SubmitHeist(request.sessionId(), playerId, 1, 100L, 0, 150L,
                999L, 1_500L, Outcome.LOST, request.completedAt());
        ApiException failure = assertThrows(ApiException.class, () -> service.submit(changed));
        assertEquals(HttpStatus.CONFLICT, failure.status());
        verifyNoInteractions(quests);
    }

    @Test
    void unknownPlayerCannotCreateResult() {
        when(players.findForUpdate(playerId)).thenReturn(Optional.empty());
        when(heists.findById(request.sessionId())).thenReturn(Optional.empty());
        ApiException failure = assertThrows(ApiException.class, () -> service.submit(request));
        assertEquals(HttpStatus.NOT_FOUND, failure.status());
        verify(heists, never()).insertIfAbsent(any(), any(), anyInt(), anyLong(), anyInt(), anyLong(),
                anyLong(), anyLong(), anyString(), any(), any());
    }

    @Test
    void retryUsesDatabaseTimestampPrecision() {
        SubmitHeist precise = new SubmitHeist(request.sessionId(), playerId, 1, 100L, 0, 150L,
                -50L, 1_500L, Outcome.LOST, Instant.parse("2026-10-18T14:59:59.123456789Z"));
        HeistSession stored = new HeistSession(precise, NOW);
        assertEquals(Instant.parse("2026-10-18T14:59:59.123456Z"), stored.getCompletedAt());
        assertTrue(stored.matches(precise));
    }

    private void whenInsertReturns(int result) {
        when(heists.insertIfAbsent(any(), any(), anyInt(), anyLong(), anyInt(), anyLong(),
                anyLong(), anyLong(), anyString(), any(), any())).thenReturn(result);
    }
}
