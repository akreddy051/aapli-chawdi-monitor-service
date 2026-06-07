package com.example.aapliChawdi.service;

import com.example.aapliChawdi.entity.Subscription;
import com.example.aapliChawdi.entity.Village;
import com.example.aapliChawdi.repository.SubscriptionRepositoryInterface;
import com.example.aapliChawdi.repository.VillageRepositoryInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService implements SubscriptionServiceInterface {

    private final SubscriptionRepositoryInterface subscriptionRepository;
    private final VillageRepositoryInterface villageRepository;

    public void subscribe(
            Long chatId,
            String district,
            String taluka,
            String villageName) {

        Village village = villageRepository
                .findByDistrictAndTalukaAndVillage(
                        district,
                        taluka,
                        villageName
                )
                .orElseGet(() -> {
                    Village newVillage = Village.builder()
                            .district(district)
                            .taluka(taluka)
                            .village(villageName)
                            .build();

                    return villageRepository.save(newVillage);
                });

        boolean alreadySubscribed =
                subscriptionRepository
                        .existsByChatIdAndVillage(
                                chatId,
                                village
                        );

        if (alreadySubscribed) {
            return;
        }

        Subscription subscription =
                Subscription.builder()
                        .chatId(chatId)
                        .village(village)
                        .build();

        subscriptionRepository.save(subscription);
    }

    public void unsubscribe(
            Long chatId,
            String district,
            String taluka,
            String villageName) {

        villageRepository
                .findByDistrictAndTalukaAndVillage(
                        district,
                        taluka,
                        villageName
                )
                .ifPresent(village ->
                        subscriptionRepository
                                .deleteByChatIdAndVillage(
                                        chatId,
                                        village
                                )
                );
    }

    public List<Subscription> getSubscriptions(
            Long chatId) {

        return subscriptionRepository.findByChatId(chatId);
    }

    public List<Subscription> getSubscribers(
            Village village) {

        return subscriptionRepository.findByVillage(village);
    }

    public List<Subscription> getAllSubscriptions() {

        return subscriptionRepository.findAll();
    }

    public void unsubscribeById(Long subscriptionId) {
        subscriptionRepository.deleteById(subscriptionId);
    }
}
