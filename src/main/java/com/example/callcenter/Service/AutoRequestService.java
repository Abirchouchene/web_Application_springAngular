package com.example.callcenter.Service;

import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AutoRequestService {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private RestTemplate restTemplate;

    // Appelé tous les jours à 2h du matin (cron configurable)
    @Scheduled(cron = "0 0 0 * * MON")
    public void generateAutomaticRequests() {

        List<Contact> contacts = fetchContactsFromSales(); // ou SAV
        List<Question> questions = fetchFeedbackQuestionsForSAV();

        for (Contact contact : contacts) {
            Request request = new Request();
            request.setDescription("Demande automatique générée");
            request.setStatus(Status.AUTO_GENERATED);
            request.setRequestType(RequestType.STATISTICS);
            request.setCatgoryRequest(CatgoryRequest.RECALAMATION);
            request.setPriority(Priority.URGENT);
            request.setContacts(new HashSet<>(List.of(contact)));       // ✅ ici
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


    private List<Question> fetchFeedbackQuestionsForSAV() {
        Question q1 = new Question("Êtes-vous satisfait du produit reçu ?", QuestionType.YES_OR_NO);
        Question q2 = new Question("L'intervenant était-il professionnel ?", QuestionType.YES_OR_NO);
        Question q3 = new Question("La réclamation a-t-elle été bien traitée ?", QuestionType.YES_OR_NO);
        Question q4 = new Question("Notez votre satisfaction globale (1 à 10)", QuestionType.NUMBER);
        return List.of(q1, q2, q3, q4);
    }

}
