package com.productos.mari.domain.marketing;

import java.util.List;

public interface AdRequestService {
    AdRequest createRequest(AdRequest request);
    List<AdRequest> getAllRequests(String search, String status);
    AdRequest updateStatus(Long id, AdRequestStatus status);
    void deleteRequest(Long id);
}
