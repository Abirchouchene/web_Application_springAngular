package com.example.callcenter.Service;

import com.example.callcenter.DTO.AgentAvailabilityDTO;
import com.example.callcenter.DTO.RequestDTO;
import com.example.callcenter.DTO.UpdateRequestDTO;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final SubmissionRepository submissionRepository;



    public Request submitRequest(RequestDTO requestDTO) {
        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Request request = new Request();
        request.setUser(user);
        request.setRequestType(requestDTO.getRequestType());
        request.setStatus(Status.PENDING);
        request.setDescription(requestDTO.getDescription());
        request.setPriority(requestDTO.getPriorityLevel());
        request.setCategoryRequest(requestDTO.getCategory());
        request.setDeadline(requestDTO.getDeadline());

        Set<Question> questions = new HashSet<>();

        // Existing question IDs
        if (requestDTO.getQuestionIds() != null) {
            questions.addAll(questionRepository.findAllById(requestDTO.getQuestionIds()));
        }

        // New questions
        if (requestDTO.getNewQuestions() != null && !requestDTO.getNewQuestions().isEmpty()) {
            List<Question> newQuestionEntities = requestDTO.getNewQuestions().stream().map(dto -> {
                Question question = new Question();
                question.setText(dto.getText());
                question.setQuestionType(dto.getType());

                question.setOptions(dto.getOptions());
                return question;
            }).collect(Collectors.toList());

            questions.addAll(questionRepository.saveAll(newQuestionEntities));
        }

        request.setQuestions(questions);


        // ✅ Save the request first to generate its ID
        requestRepository.save(request);

        // ✅ Create Submissions for each contact
        List<Contact> contacts = contactRepository.findAllById(requestDTO.getContactIds());
        if (contacts.size() != requestDTO.getContactIds().size()) {
            throw new RuntimeException("One or more contacts not found.");
        }

        List<Submission> submissions = contacts.stream().map(contact -> {
            Submission submission = new Submission();
            submission.setContact(contact);
            submission.setRequest(request);
            submission.setSubmissionDate(LocalDate.now());
            return submission;
        }).collect(Collectors.toList());

        submissionRepository.saveAll(submissions);

        return request;
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

        // ✅ Update Submissions (Contacts)
        if (dto.getContactIds() != null && !dto.getContactIds().isEmpty()) {
            // Delete old submissions
            submissionRepository.deleteByRequest(existingRequest);

            // Create new submissions
            List<Contact> contacts = contactRepository.findAllById(dto.getContactIds());
            if (contacts.size() != dto.getContactIds().size()) {
                throw new RuntimeException("One or more contacts not found.");
            }

            List<Submission> newSubmissions = contacts.stream().map(contact -> {
                Submission submission = new Submission();
                submission.setRequest(existingRequest);
                submission.setContact(contact);
                submission.setSubmissionDate(LocalDate.now());
                return submission;
            }).collect(Collectors.toList());

            submissionRepository.saveAll(newSubmissions);
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
    public Request updateNote(Long id, String note) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Request not found with id: " + id));

        request.setNote(note);
        return requestRepository.save(request);
    }


}







