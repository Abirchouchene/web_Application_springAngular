package com.example.callcenter.Service;

import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.List;

@Service
public class AutoRequestService {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private RestTemplate restTemplate;

    // Appelé tous les jours à 2h du matin (cron configurable)
    @Scheduled(cron = "0 0 0 * * MON")
    public void generateAutomaticRequests() {
        List<Contact> contacts = fetchContactsFromSales(); // or SAV
        List<Question> questions = fetchQuestionsFromReclamationRequests(); // <== updated here

        for (Contact contact : contacts) {
            Request request = new Request();
            request.setDescription("Demande automatique générée");
            request.setStatus(Status.AUTO_GENERATED);
            request.setRequestType(RequestType.STATISTICS);
            request.setCategoryRequest(CategoryRequest.RECLAMATION);
            request.setPriority(Priority.URGENT);
            request.setContacts(new HashSet<>(List.of(contact)));
            request.setQuestions(new HashSet<>(questions));
            requestRepository.save(request);
        }

        System.out.println("✅ Demandes automatiques générées avec succès.");
    }
    private List<Contact> fetchContactsFromSales() {
        Contact contact1 = new Contact();
        contact1.setIdC(1L);
        contact1.setName("Ali Ben Salah");
        contact1.setPhoneNumber("98765432");

        Contact contact2 = new Contact();
        contact2.setIdC(2L);
        contact2.setName("Sara Meftah");
        contact2.setPhoneNumber("12345678");

        return List.of(contact1, contact2);
    }



    private List<Question> fetchQuestionsFromReclamationRequests() {
        return requestRepository.findQuestionsByReclamationRequests();
    }
}
