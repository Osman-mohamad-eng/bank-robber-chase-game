package dev.bankheist.api.heist;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** Client-reported terminal snapshot; this service does not replay gameplay or calculate its score. */
public record SubmitHeist(
        @NotNull UUID sessionId,
        @NotNull UUID playerId,
        @NotNull @Min(1) Integer level,
        @NotNull @Min(0) Long cashCollected,
        @NotNull @Min(0) Integer diamondsCollected,
        @NotNull @Min(0) Long penaltiesIncurred,
        @NotNull Long finalScore,
        @NotNull @Min(0) Long completionTimeMs,
        @NotNull Outcome outcome,
        @NotNull Instant completedAt) {}
