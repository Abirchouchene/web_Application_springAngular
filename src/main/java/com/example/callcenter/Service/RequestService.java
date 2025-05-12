package com.example.callcenter.Service;

import com.example.callcenter.DTO.AgentAvailabilityDTO;
import com.example.callcenter.DTO.QuestionDTO;
import com.example.callcenter.DTO.RequestDTO;
import com.example.callcenter.DTO.UpdateRequestDTO;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.*;
import lombok.RequiredArgsConstructor;
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
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class RequestService {
    private final RequestRepository requestRepository;

    private final UserRepository userRepository;
    private  final ContactRepository contactRepository;
    private final QuestionRepository questionRepository;
    private final AgentLeaveRepository agentLeaveRepository;

    @Value("${file.upload-dir}")
    private String UPLOAD_DIR;

    // Inside RequestService.java
    public Request submitRequest(RequestDTO requestDTO) {
        // Retrieve the user from the database
        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create a new Request entity
        Request request = new Request();
        request.setUser(user);
        request.setRequestType(requestDTO.getRequestType());
        request.setStatus(Status.PENDING);
        request.setDescription(requestDTO.getDescription());
        request.setPriority(requestDTO.getPriorityLevel());
        request.setCategoryRequest(requestDTO.getCategory());
        request.setDeadline(requestDTO.getDeadline());

        // Convert List<Contact> to Set<Contact>
        Set<Contact> contactSet = new HashSet<>(contactRepository.findAllById(requestDTO.getContactIds()));

        // Check if the result set matches the input size
        if (contactSet.size() != requestDTO.getContactIds().size()) {
            throw new RuntimeException("One or more contacts not found.");
        }

        // Set the contacts to the request
        request.setContacts(contactSet);

        // Handle existing and new questions
        Set<Question> questions = new HashSet<>();

        if (requestDTO.getQuestionIds() != null) {
            questions.addAll(questionRepository.findAllById(requestDTO.getQuestionIds()));
        }

        if (requestDTO.getNewQuestions() != null && !requestDTO.getNewQuestions().isEmpty()) {
            List<Question> newQuestionEntities = requestDTO.getNewQuestions().stream().map(dto -> {
                Question question = new Question();
                question.setText(dto.getText());
                question.setQuestionType(dto.getType());
                return question;
            }).collect(Collectors.toList());

            questions.addAll(questionRepository.saveAll(newQuestionEntities));
        }

        request.setQuestions(questions);

        // No file handling anymore

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
    @Transactional
    public Request updateRequestByRequester(Long requestId, UpdateRequestDTO dto, Long requesterId) {
        Request existingRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!existingRequest.getUser().getIdUser().equals(requesterId)) {
            throw new IllegalArgumentException("You are not allowed to modify this request.");
        }

        if (existingRequest.getStatus() != Status.PENDING) {
            throw new IllegalStateException("Only pending requests can be modified.");
        }

        // ✅ Basic fields
        existingRequest.setDescription(dto.getDescription());
        existingRequest.setPriority(dto.getPriority());
        existingRequest.setCategoryRequest(dto.getCategoryRequest());

        if (dto.getDeadline() != null) {
            existingRequest.setDeadline(dto.getDeadline());
        }

        // ✅ Set Contacts
        if (dto.getContactIds() != null && !dto.getContactIds().isEmpty()) {
            List<Contact> contacts = contactRepository.findAllById(dto.getContactIds());
            existingRequest.setContacts(new HashSet<>(contacts));
        }

        // ✅ Set Questions
        if (dto.getQuestionIds() != null && !dto.getQuestionIds().isEmpty()) {
            List<Question> questions = questionRepository.findAllById(dto.getQuestionIds());
            existingRequest.setQuestions(new HashSet<>(questions));
        }

        return requestRepository.save(existingRequest);
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

    public List<Request> getRequestsByUserId(Long userId) {
        return requestRepository.findByUserIdUser(userId);
    }


    public List<Contact> searchContactsByTag(String tag) {
        return contactRepository.findByTagNameLike(tag);
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
   /* public Request updateRequestStatus(Long requestId, String status, String note) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        request.setStatus(Status.valueOf(status));
        request.setNote(note);

        return requestRepository.save(request);
    }*/
    @Transactional
    public void deleteRequest(Long id) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // Break association
        request.getQuestions().clear();
        requestRepository.save(request); // optional but safe

        // Now delete
        requestRepository.delete(request);
    }

    public List<AgentAvailabilityDTO> getAllAgentsWithAvailability(LocalDate selectedDate) {
        List<AgentLeave> leavesOnDate = agentLeaveRepository.findByDate(selectedDate);
        Map<Long, AgentLeave> leaveMap = leavesOnDate.stream()
                .collect(Collectors.toMap(l -> l.getAgent().getIdUser(), l -> l)); // map agentId -> leave

        List<User> allAgents = userRepository.findAllAgents();

        return allAgents.stream()
                .map(agent -> {
                    AgentAvailabilityDTO dto = new AgentAvailabilityDTO();
                    dto.setAgentId(agent.getIdUser());
                    dto.setAgentName(agent.getFullName());

                    AgentLeave leave = leaveMap.get(agent.getIdUser());
                    boolean isAvailable = (leave == null);
                    dto.setAvailable(isAvailable);

                    if (leave != null) {
                        dto.setLeaveStartDate(leave.getStartDate());
                        dto.setLeaveEndDate(leave.getEndDate());
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public Question createNewQuestion(String questionText, QuestionType questionType) {
        Question question = new Question(questionText, questionType);
        return questionRepository.save(question);
    }

}







