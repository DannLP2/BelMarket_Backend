package com.productos.mari.domain.support;

import com.productos.mari.domain.support.SupportRequestDto;
import com.productos.mari.domain.support.SupportRequest;

public interface SupportService {
    SupportRequestDto processSupportRequest(SupportRequestDto dto, org.springframework.web.multipart.MultipartFile attachment) throws java.io.IOException;
    java.util.List<SupportRequestDto> getAllRequests();
    SupportRequestDto updateStatus(Long id, String status);
    void deleteRequest(Long id);
}
