package dev.bankheist.api.player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import dev.bankheist.api.common.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PlayerServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");
    private final PlayerRepository repository = mock(PlayerRepository.class);
    private final PlayerService service = new PlayerService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void registersCaseInsensitiveUsernameWithoutChangingDisplayName() {
        UUID id = UUID.randomUUID();
        when(repository.register(any(), eq("Player_1"), eq("player_1"), eq(NOW))).thenReturn(1);
        when(repository.findByNormalizedUsername("player_1"))
                .thenReturn(Optional.of(new Player(id, "Player_1", "player_1", NOW)));

        PlayerService.Registration result = service.register(null, "Player_1");

        assertTrue(result.created());
        assertEquals(id, result.player().id());
        assertEquals("Player_1", result.player().username());
    }

    @Test
    void acceptsPersistedClientIdentityForOfflineFirstPlay() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(new Player(id, "Player_1", "player_1", NOW)));

        PlayerService.Registration result = service.register(id, "PLAYER_1");

        assertFalse(result.created());
        assertEquals(id, result.player().id());
    }

    @Test
    void rejectsReusingPlayerIdForDifferentUsername() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(new Player(id, "Original", "original", NOW)));
        ApiException failure = assertThrows(ApiException.class, () -> service.register(id, "SomeoneElse"));
        assertEquals(HttpStatus.CONFLICT, failure.status());
    }

    @Test
    void rejectsClaimingExistingUsernameWithDifferentId() {
        UUID id = UUID.randomUUID();
        ApiException failure = assertThrows(ApiException.class, () -> service.register(id, "TakenName"));
        assertEquals(HttpStatus.CONFLICT, failure.status());
    }
}
