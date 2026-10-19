package dev.bankheist.api.heist;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/heists")
public class HeistController {
    private final HeistService service;

    public HeistController(HeistService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<HeistView> submit(@Valid @RequestBody SubmitHeist request) {
        HeistService.Submission result = service.submit(request);
        return result.created()
                ? ResponseEntity.created(URI.create("/api/v1/heists/" + result.heist().sessionId())).body(result.heist())
                : ResponseEntity.ok(result.heist());
    }

    @GetMapping("/{sessionId}")
    public HeistView get(@PathVariable UUID sessionId) {
        return service.get(sessionId);
    }
}
