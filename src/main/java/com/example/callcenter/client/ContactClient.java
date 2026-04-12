package com.example.callcenter.client;

import com.example.callcenter.Config.ContactClientConfig;
import com.example.callcenter.DTO.ContactResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "contact-service", configuration = ContactClientConfig.class)
public interface ContactClient {
    @GetMapping("/api/contacts/{contactId}")
    ContactResponse getContactById(@PathVariable("contactId") Long id);

    @GetMapping("/api/contacts")
    List<ContactResponse> getAllContacts();

    @GetMapping("/api/contacts/tags/searchByTag")
    List<ContactResponse> getContactsByTag(@RequestParam("tag") String tag);
    
    @GetMapping("/api/callbacks/upcoming")
    List<Map<String, Object>> getUpcomingCallbacks();
} 