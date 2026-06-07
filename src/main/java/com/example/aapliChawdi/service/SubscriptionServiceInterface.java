package com.example.aapliChawdi.service;

import com.example.aapliChawdi.entity.Subscription;
import com.example.aapliChawdi.entity.Village;

import java.util.List;

public interface SubscriptionServiceInterface {
    void subscribe(Long chatId, String district, String taluka, String village);

    void unsubscribe(Long chatId, String district, String taluka, String village);

    List<Subscription> getSubscriptions(Long chatId);

    List<Subscription> getSubscribers(Village village);

    List<Subscription> getAllSubscriptions();

    void unsubscribeById(Long subscriptionId);
}
