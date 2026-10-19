package dev.bankheist.api.heist;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.bankheist.api.common.ApiException;
import dev.bankheist.api.common.ApiExceptionHandler;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HeistControllerTest {
    private static final String JSON = """
            {"sessionId":"11111111-1111-1111-1111-111111111111",
             "playerId":"22222222-2222-2222-2222-222222222222", "level":1,
             "cashCollected":100,"diamondsCollected":0,"penaltiesIncurred":150,
             "finalScore":-50,"completionTimeMs":1500,"outcome":"LOST",
             "completedAt":"2026-10-18T12:00:00Z"}
            """;
    private final HeistService service = mock(HeistService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new HeistController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void createdResultHasLocationAndResultBody() throws Exception {
        UUID session = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID player = UUID.fromString("22222222-2222-2222-2222-222222222222");
        HeistView view = new HeistView(session, player, "Player", 1, 100, 0, 150, -50,
                1500, Outcome.LOST, Instant.parse("2026-10-18T12:00:00Z"), Instant.now());
        when(service.submit(any())).thenReturn(new HeistService.Submission(view, true));

        mvc.perform(post("/api/v1/heists").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/heists/" + session))
                .andExpect(jsonPath("$.finalScore").value(-50));
    }

    @Test
    void rejectsNegativeDurationBeforeServiceCall() throws Exception {
        mvc.perform(post("/api/v1/heists").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.replace("1500", "-1")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingRequiredPrimitiveInsteadOfDefaultingToZero() throws Exception {
        mvc.perform(post("/api/v1/heists").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.replace("\"cashCollected\":100,", "")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void conflictingIdHasUsefulJsonError() throws Exception {
        when(service.submit(any())).thenThrow(ApiException.conflict("Session ID already belongs to a different result"));
        mvc.perform(post("/api/v1/heists").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").exists());
    }

    @Test
    void rejectsNegativeCashAndUnknownOutcome() throws Exception {
        mvc.perform(post("/api/v1/heists").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.replace("\"cashCollected\":100", "\"cashCollected\":-1")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/heists").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.replace("\"LOST\"", "\"ABANDONED\"")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void unknownPlayerHasUsefulNotFoundResponse() throws Exception {
        when(service.submit(any())).thenThrow(ApiException.notFound("Player not found"));
        mvc.perform(post("/api/v1/heists").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Player not found"));
    }
}
