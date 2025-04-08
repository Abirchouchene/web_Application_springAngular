package com.example.callcenter.Service;

import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Set;

@Service
public class AutoRequestService {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private RestTemplate restTemplate;

    // Appelé tous les jours à 2h du matin (cron configurable)
    @Scheduled(cron = "0 0 2 * * *")
    public void generateAutomaticRequests() {
        List<Contact> contacts = fetchContactsFromSales(); // ou SAV
        List<Question> questions = fetchPredefinedQuestions();

        for (Contact contact : contacts) {
            Request request = new Request();
            request.setDescription("Demande automatique générée");
            request.setStatus(Status.AUTO_GENERATED);
            request.setRequestType(RequestType.STATISTICS);
            request.setCatgoryRequest(CatgoryRequest.PRODUIT);
            request.setPriority(Priority.URGENT);
            request.setContacts((Set<Contact>) List.of(contact));
            request.setQuestions((Set<Question>) questions);

            requestRepository.save(request);
        }

        System.out.println("✅ Demandes automatiques générées avec succès.");
    }

    private List<Contact> fetchContactsFromSales() {
        String url = "http://localhost:8081/api/contacts"; // Modifie l’URL selon ton projet
        Contact[] response = restTemplate.getForObject(url, Contact[].class);
        return List.of(response);
    }

    private List<Question> fetchPredefinedQuestions() {
        // Simuler avec des données mockées ou appeler un service
        Question q1 = new Question("Le produit est-il satisfaisant ?", QuestionType.YES_OR_NO);
        Question q2 = new Question("Quelle est votre note ?", QuestionType.NUMBER);
        return List.of(q1, q2);
    }
}
