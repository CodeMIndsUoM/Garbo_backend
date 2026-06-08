package com.garbo.infrastructure.config;

import com.garbo.core.service.shared.CollectionRequestService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CollectionOfferMaintenanceScheduler {

    private final CollectionRequestService collectionRequestService;

    public CollectionOfferMaintenanceScheduler(CollectionRequestService collectionRequestService) {
        this.collectionRequestService = collectionRequestService;
    }

    @Scheduled(fixedDelay = 6 * 60 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void expireStalePendingOffers() {
        int expired = collectionRequestService.expireStalePendingOffers();
        if (expired > 0) {
            System.out.println("Expired " + expired + " stale collection offers.");
        }
    }
}
