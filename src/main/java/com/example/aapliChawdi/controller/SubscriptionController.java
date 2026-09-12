package com.example.aapliChawdi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.aapliChawdi.dto.SubscriptionRequest;
import com.example.aapliChawdi.entity.Subscription;
import com.example.aapliChawdi.service.SubscriptionServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Subscriptions", description = "Manage Telegram subscriptions to villages.")
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionServiceInterface subscriptionService;

    @Operation(summary = "Subscribe to a village", description = "Uses exact location labels. Creates the village if missing; an existing subscription is left unchanged.")
    @PostMapping
    public ResponseEntity<Void> subscribe(
            @RequestBody SubscriptionRequest request) {

        subscriptionService.subscribe(
                request.getChatId(),
                request.getDistrict(),
                request.getTaluka(),
                request.getVillage()
        );

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "List subscriptions for a Telegram chat")
    @GetMapping("/{chatId}")
    public ResponseEntity<List<Subscription>> getSubscriptions(
            @PathVariable Long chatId) {

        return ResponseEntity.ok(
                subscriptionService.getSubscriptions(chatId)
        );
    }

    @Operation(summary = "Unsubscribe from a village", description = "Removes the subscription matching the chat and exact location labels.")
    @DeleteMapping
    public ResponseEntity<Void> unsubscribe(
            @RequestBody SubscriptionRequest request) {

        subscriptionService.unsubscribe(
                request.getChatId(),
                request.getDistrict(),
                request.getTaluka(),
                request.getVillage()
        );

        return ResponseEntity.ok().build();
    }
}