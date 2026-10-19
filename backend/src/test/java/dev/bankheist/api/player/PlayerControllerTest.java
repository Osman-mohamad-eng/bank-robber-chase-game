package dev.bankheist.api.player;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.bankheist.api.common.ApiExceptionHandler;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PlayerControllerTest {
    private final PlayerService service = mock(PlayerService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new PlayerController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void acceptsClientUuidAndReturnsCreatedProfile() throws Exception {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(service.register(id, "Player_1")).thenReturn(new PlayerService.Registration(
                new PlayerService.PlayerView(id, "Player_1", Instant.parse("2026-10-18T12:00:00Z"), 0), true));

        mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerId\":\"" + id + "\",\"username\":\"Player_1\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void rejectsTooShortAndInvalidUsername() throws Exception {
        mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"ab\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/players").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"Player Name\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
