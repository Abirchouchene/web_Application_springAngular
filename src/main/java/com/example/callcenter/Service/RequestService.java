package com.example.callcenter.Service;

import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.ContactRepository;
import com.example.callcenter.Repository.QuestionRepository;
import com.example.callcenter.Repository.RequestRepository;
import com.example.callcenter.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class RequestService {
    private final RequestRepository requestRepository;

    private final UserRepository userRepository;

    private  final ContactRepository contactRepository;
    private final QuestionRepository questionRepository;

    @Value("${file.upload-dir}")
    private String UPLOAD_DIR;

    public Request submitRequest(Long userId, RequestType requestType, List<Long> contactIds,
                                 String description, CatgoryRequest category,
                                 List<Long> questionIds, List<String> newQuestions,
                                 Priority priorityLevel, QuestionType defaultQuestionType,
                                 MultipartFile file) {

        // Retrieve the user who is submitting the request
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create a new request instance
        Request request = new Request();
        request.setUser(user);
        request.setRequestType(requestType);
        request.setStatus(Status.PENDING);
        request.setDescription(description);
        request.setPriority(priorityLevel);
        request.setCatgoryRequest(category);

        // Define questions set
        Set<Question> questions = new HashSet<>();

        if (requestType == RequestType.RECLAMATION) {
            if (contactIds.size() != 1) {
                throw new IllegalArgumentException("Only one contact can be associated with a Reclamation request.");
            }
            Contact contact = contactRepository.findById(contactIds.get(0))
                    .orElseThrow(() -> new RuntimeException("Contact not found"));
            request.getContacts().add(contact);

        } else if (requestType == RequestType.STATISTICS) {
            if (contactIds.isEmpty()) {
                throw new IllegalArgumentException("At least one contact must be associated with a Statistics request.");
            }
            List<Contact> contacts = contactRepository.findAllById(contactIds);
            if (contacts.size() != contactIds.size()) {
                throw new RuntimeException("One or more contacts not found.");
            }
            request.getContacts().addAll(contacts);

            // Handle existing questions
            if (questionIds != null && !questionIds.isEmpty()) {
                questions.addAll(questionRepository.findAllById(questionIds));
            }

            // Handle new questions
            if (newQuestions != null && !newQuestions.isEmpty()) {
                List<Question> newQuestionEntities = newQuestions.stream().map(q -> {
                    Question question = new Question();
                    question.setQuestion(q);
                    question.setQuestionType(defaultQuestionType); // Use passed default type
                    return question;
                }).collect(Collectors.toList());

                questions.addAll(questionRepository.saveAll(newQuestionEntities));
            }

            request.setQuestions(questions);
        }

        // Handle file upload (try-catch block to catch IOException)
        if (file != null && !file.isEmpty()) {
            try {
                String attachmentPath = saveFile(file);
                request.setAttachmentPath(attachmentPath); // Set the file path in the request
            } catch (IOException e) {
                throw new RuntimeException("File upload failed", e); // Handle the exception (e.g., log, rethrow)
            }
        }

        return requestRepository.save(request);
    }


    private String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        // Ensure upload directory exists
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // Generate unique file name and save it
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(UPLOAD_DIR, fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString(); // Return saved file path
    }

    public List<Request> getAllRequests() {
        return requestRepository.findAll();
    }
    public List<Request> getRequestsByType(RequestType requestType) {
        return requestRepository.findByRequestType(requestType);
    }
  public  List<Contact>getAllContacts(){
        return contactRepository.findAll();
  }
    public Request getRequestById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
    }
    public Contact addTagToContact(Long contactId, String tag) {
        Optional<Contact> contact = contactRepository.findById(contactId);
        if (contact.isPresent()) {
            Contact existingContact = contact.get();

            // Ensure the tags set is initialized
            if (existingContact.getTags() == null) {
                existingContact.setTags(new HashSet<>()); // Initialize if null
            }

            existingContact.getTags().add(tag);  // Now it's safe to add the tag
            return contactRepository.save(existingContact);
        }
        return null; // Return null if contact not found
    }



    public List<Contact> searchContactsByTag(String tag) {
        return contactRepository.findByTagsContaining(tag);
    }


    public Request approveRequest(Long requestId, Status status) {
        if (status != Status.APPROVED && status != Status.REJECTED) {
            throw new IllegalArgumentException("Invalid status. Use APPROVED or REJECTED.");
        }

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        request.setStatus(status);
        return requestRepository.save(request);
    }
    public Request assignRequestToAgent(Long requestId, Long agentId) {
        // Find the request by ID
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // Find the agent by ID
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        // Check if the user is an agent
        if (!agent.getRole().equals(Role.AGENT)) {
            throw new IllegalArgumentException("User is not an agent.");
        }

        // Set the agent to the request
        request.setAgent(agent);

        // Change the status to "assigned"
        request.setStatus(Status.ASSIGNED);

        // Save the updated request and return it
        return requestRepository.save(request);
    }



    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);  // Fetch users based on role
    }
    public List<Request> getRequestsAssignedToAgent(Long agentId) {
        return requestRepository.findByAgent_IdUser(agentId);

    }
    @Transactional
    public Request updateRequestStatus(Long requestId, Status newStatus) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));




        request.setStatus(newStatus);

        return requestRepository.save(request);
    }
    public Request updateRequestStatus(Long requestId, String status, String note) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        request.setStatus(Status.valueOf(status));
        request.setNote(note);

        return requestRepository.save(request);
    }

}







