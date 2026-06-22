package com.productos.mari.domain.marketing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketingSchedulerTest {

    @Mock
    private BannerRepository bannerRepository;
    @Mock
    private AdRequestRepository adRequestRepository;
    @Mock
    private AdvertiserRepository advertiserRepository;

    @InjectMocks
    private MarketingScheduler scheduler;

    @Test
    void processExpirations_shouldDeactivateExpiredBanners() {
        Banner expired = Banner.builder().id(1L).active(true).endDate(LocalDateTime.now().minusDays(1)).build();
        Banner valid = Banner.builder().id(2L).active(true).endDate(LocalDateTime.now().plusDays(1)).build();
        
        when(bannerRepository.findAll()).thenReturn(List.of(expired, valid));

        scheduler.processExpirations();

        assertFalse(expired.isActive());
        verify(bannerRepository, times(1)).saveAll(argThat(list -> ((List<Banner>)list).contains(expired) && !((List<Banner>)list).contains(valid)));
    }

    @Test
    void processExpirations_shouldMarkExpiredAdRequests() {
        AdRequest expired = AdRequest.builder().id(1L).status(AdRequestStatus.APPROVED).endDate(LocalDateTime.now().minusDays(1)).build();
        AdRequest valid = AdRequest.builder().id(2L).status(AdRequestStatus.APPROVED).endDate(LocalDateTime.now().plusDays(1)).build();
        
        when(adRequestRepository.findAll()).thenReturn(List.of(expired, valid));

        scheduler.processExpirations();

        assertEquals(AdRequestStatus.EXPIRED, expired.getStatus());
        verify(adRequestRepository, times(1)).saveAll(argThat(list -> ((List<AdRequest>)list).contains(expired) && !((List<AdRequest>)list).contains(valid)));
    }

    @Test
    void processExpirations_shouldDeactivateExpiredAdvertisers() {
        Advertiser expired = Advertiser.builder().id(1L).active(true).endDate(LocalDateTime.now().minusDays(1)).build();
        Advertiser valid = Advertiser.builder().id(2L).active(true).endDate(LocalDateTime.now().plusDays(1)).build();
        
        when(advertiserRepository.findAll()).thenReturn(List.of(expired, valid));

        scheduler.processExpirations();

        assertFalse(expired.isActive());
        verify(advertiserRepository, times(1)).saveAll(argThat(list -> ((List<Advertiser>)list).contains(expired) && !((List<Advertiser>)list).contains(valid)));
    }

    @Test
    void processExpirations_shouldSkipWhenNothingExpired() {
        Banner validBanner = Banner.builder().id(1L).active(true).endDate(LocalDateTime.now().plusDays(1)).build();
        AdRequest validRequest = AdRequest.builder().id(1L).status(AdRequestStatus.APPROVED).endDate(LocalDateTime.now().plusDays(1)).build();
        Advertiser validAd = Advertiser.builder().id(1L).active(true).endDate(LocalDateTime.now().plusDays(1)).build();

        when(bannerRepository.findAll()).thenReturn(List.of(validBanner));
        when(adRequestRepository.findAll()).thenReturn(List.of(validRequest));
        when(advertiserRepository.findAll()).thenReturn(List.of(validAd));

        scheduler.processExpirations();

        // Nothing should be saved when nothing is expired
        verify(bannerRepository, never()).saveAll(any());
        verify(adRequestRepository, never()).saveAll(any());
        verify(advertiserRepository, never()).saveAll(any());
    }
}
