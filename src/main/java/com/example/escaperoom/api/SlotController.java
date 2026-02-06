package com.example.escaperoom.api;

import com.example.escaperoom.api.dto.HoldResponse;
import com.example.escaperoom.model.Slot;
import com.example.escaperoom.service.SlotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class SlotController {

    private final SlotService service;

    public SlotController(SlotService service) {
        this.service = service;
    }

    @GetMapping(value = "/slots/{slotId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Slot> getSlot(@PathVariable Long slotId) {
        return ResponseEntity.ok(service.getSlot(slotId));
    }

    @PostMapping(value = "/slots/{slotId}/hold", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HoldResponse> hold(@PathVariable Long slotId) {
        HoldResponse body = service.holdSlot(slotId);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping(value = "/holds/{holdId}/confirm", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> confirm(@PathVariable UUID holdId) {
        service.confirmHold(holdId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/holds/{holdId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> release(@PathVariable UUID holdId) {
        service.releaseHold(holdId);
        return ResponseEntity.noContent().build();
    }
}
