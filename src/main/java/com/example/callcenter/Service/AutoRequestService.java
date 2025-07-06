/*package com.example.callcenter.Service;

import com.example.callcenter.DTO.ContactResponse;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.RequestRepository;
import com.example.callcenter.client.ContactClient;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoRequestService {
    private final RequestRepository requestRepository;
    private final ContactClient contactClient;

    // Appelé tous les jours à 2h du matin (cron configurable)
    @Scheduled(cron = "0 0 0 * * MON")
    public void generateAutomaticRequests() {
        List<ContactResponse> contacts = fetchContactsFromSales(); // or SAV
        List<Question> questions = fetchQuestionsFromReclamationRequests();

        Request request = new Request();
        request.setDescription("Demande automatique générée");
        request.setStatus(Status.AUTO_GENERATED);
        request.setRequestType(RequestType.STATISTICS);
        request.setCategoryRequest(CategoryRequest.RECLAMATION);
        request.setPriority(Priority.URGENT);
        request.setQuestions(new HashSet<>(questions));

        // Save the request first
        Request savedRequest = requestRepository.save(request);

        // Create a Submission for each contact
        for (ContactResponse contact : contacts) {
            Submission submission = new Submission();
            submission.setRequest(savedRequest);
            submission.setContactId(contact.getIdC());
            submission.setSubmissionDate(java.time.LocalDate.now());

            savedRequest.getSubmissionList().add(submission); // Add submission to request
        }

        requestRepository.save(savedRequest); // Save again to persist submissions

        System.out.println("✅ Demandes automatiques générées avec succès.");
    }

    private List<ContactResponse> fetchContactsFromSales() {
        // In a real application, you would call the contact service to get contacts
        // For now, we'll use hardcoded IDs and fetch them from the contact service
        return List.of(
            contactClient.getContactById(1L),
            contactClient.getContactById(2L)
        );
    }

    private List<Question> fetchQuestionsFromReclamationRequests() {
        return requestRepository.findQuestionsByReclamationRequests();
    }
}
*/