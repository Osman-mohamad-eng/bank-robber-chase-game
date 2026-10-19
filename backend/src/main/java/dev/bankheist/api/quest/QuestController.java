package dev.bankheist.api.quest;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/players/{playerId}/quests")
public class QuestController {
    private final QuestService service;

    public QuestController(QuestService service) {
        this.service = service;
    }

    @GetMapping
    public QuestService.PlayerQuests get(@PathVariable UUID playerId) {
        return service.get(playerId);
    }
}
