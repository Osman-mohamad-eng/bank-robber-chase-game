package dev.bankheist.api.player;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {
    private final PlayerService service;

    public PlayerController(PlayerService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PlayerService.PlayerView> register(@Valid @RequestBody RegisterPlayer request) {
        PlayerService.Registration result = service.register(request.playerId(), request.username());
        return result.created()
                ? ResponseEntity.created(URI.create("/api/v1/players/" + result.player().id() + "/stats"))
                        .body(result.player())
                : ResponseEntity.ok(result.player());
    }

    public record RegisterPlayer(
            UUID playerId,
            @NotNull @Pattern(regexp = "[A-Za-z0-9_-]{3,32}",
                    message = "must contain 3 to 32 ASCII letters, numbers, underscores or hyphens") String username) {}
}
