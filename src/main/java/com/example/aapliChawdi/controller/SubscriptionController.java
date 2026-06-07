package com.example.aapliChawdi.controller;

import com.example.aapliChawdi.dto.SubscriptionRequest;
import com.example.aapliChawdi.entity.Subscription;
import com.example.aapliChawdi.service.SubscriptionServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionServiceInterface subscriptionService;

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

    @GetMapping("/{chatId}")
    public ResponseEntity<List<Subscription>> getSubscriptions(
            @PathVariable Long chatId) {

        return ResponseEntity.ok(
                subscriptionService.getSubscriptions(chatId)
        );
    }

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