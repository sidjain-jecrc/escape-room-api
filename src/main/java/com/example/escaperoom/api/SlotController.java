package com.example.escaperoom.api;

import com.example.escaperoom.api.dto.HoldResponse;
import com.example.escaperoom.model.Slot;
import com.example.escaperoom.service.SlotService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class SlotController {

    private final SlotService service;

    public SlotController(SlotService service) {
        this.service = service;
    }

    @GetMapping("/slots/{slotId}")
    public Slot getSlot(@PathVariable Long slotId) {
        return service.getSlot(slotId);
    }

    @PostMapping("/slots/{slotId}/hold")
    public HoldResponse hold(@PathVariable Long slotId) {
        return service.holdSlot(slotId);
    }

    @PostMapping("/holds/{holdId}/confirm")
    public void confirm(@PathVariable UUID holdId) {
        service.confirmHold(holdId);
    }

    @DeleteMapping("/holds/{holdId}")
    public void release(@PathVariable UUID holdId) {
        service.releaseHold(holdId);
    }
}
