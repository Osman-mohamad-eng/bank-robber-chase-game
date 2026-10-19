package dev.bankheist.api.leaderboard;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.bankheist.api.common.ApiExceptionHandler;
import dev.bankheist.api.common.ResultPage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LeaderboardControllerTest {
    private final LeaderboardService service = mock(LeaderboardService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new LeaderboardController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        when(service.leaderboard(any(), nullable(Integer.class), anyInt(), anyInt()))
                .thenReturn(new ResultPage<>(List.of(), 20, 0, 0));
    }

    @Test
    void forwardsLevelFilterAndUsesBoundedDefaultPage() throws Exception {
        mvc.perform(get("/api/v1/leaderboards/money").param("level", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isArray());
        verify(service).leaderboard(LeaderboardMetric.MONEY, 2, 20, 0);
    }

    @Test
    void rejectsOversizedPage() throws Exception {
        mvc.perform(get("/api/v1/leaderboards/time").param("limit", "101"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsNegativeOffset() throws Exception {
        mvc.perform(get("/api/v1/leaderboards/score").param("offset", "-1"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
