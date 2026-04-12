package com.example.callcenter.Service;

import com.example.callcenter.DTO.AgentAvailabilityDTO;
import com.example.callcenter.DTO.ContactResponse;
import com.example.callcenter.DTO.RequestDTO;
import com.example.callcenter.DTO.UpdateRequestDTO;
import com.example.callcenter.Entity.*;
import com.example.callcenter.Repository.*;
import com.example.callcenter.client.ContactClient;
import com.example.callcenter.Service.RequestContactStatusService;
import com.example.callcenter.Service.LogsService;
import lombok.RequiredArgsConstructor;
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
    private final QuestionRepository questionRepository;
    private final AgentLeaveRepository agentLeaveRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionService submissionService;
    private final ContactClient contactClient;
    private final RequestContactStatusService requestContactStatusService;
    private final LogsService logsService;
    private final ReportRepository reportRepository;

    @Transactional
    public Request submitRequest(RequestDTO requestDTO) {
        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Request request = new Request();
        request.setUser(user);
        request.setTitle(requestDTO.getTitle());
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

        request.setQuestions(new ArrayList<>(questions));

        // Save the request first to generate its ID
        requestRepository.save(request);

        // Create Submissions directly from contactIds (no need to call contact-service)
        List<Long> contactIds = requestDTO.getContactIds();
        if (contactIds != null && !contactIds.isEmpty()) {
            List<Submission> submissions = contactIds.stream().map(contactId -> {
                Submission submission = new Submission();
                submission.setContactId(contactId);
                submission.setRequest(request);
                submission.setSubmissionDate(LocalDate.now());
                return submission;
            }).collect(Collectors.toList());

            submissionRepository.saveAll(submissions);

            // Initialize contact statuses for this request
            requestContactStatusService.initializeContactStatuses(request.getIdR(), contactIds);
        }

        // Log the request creation
        logsService.logRequestCreated(request, user, "127.0.0.1", "System");

        // Force-initialize lazy collections before leaving transaction
        request.getSubmissionList().size();
        request.getQuestions().size();
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

        // Basic fields
        existingRequest.setDescription(dto.getDescription());
        existingRequest.setPriority(dto.getPriority());
        existingRequest.setCategoryRequest(dto.getCategoryRequest());

        if (dto.getDeadline() != null) {
            existingRequest.setDeadline(dto.getDeadline());
        }

        // Update Submissions (Contacts) - use contactIds directly, no Feign call needed
        if (dto.getContactIds() != null && !dto.getContactIds().isEmpty()) {
            // Delete old submissions using SubmissionService
            submissionService.deleteSubmissionsByRequestId(existingRequest.getIdR());

            // Create new submissions directly from contactIds
            List<Submission> newSubmissions = dto.getContactIds().stream().map(contactId -> {
                Submission submission = new Submission();
                submission.setRequest(existingRequest);
                submission.setContactId(contactId);
                submission.setSubmissionDate(LocalDate.now());
                return submission;
            }).collect(Collectors.toList());

            submissionRepository.saveAll(newSubmissions);
        }

        // Set Questions
        if (dto.getQuestionIds() != null && !dto.getQuestionIds().isEmpty()) {
            List<Question> questions = questionRepository.findAllById(dto.getQuestionIds());
            existingRequest.setQuestions(questions);
        }

        Request saved = requestRepository.save(existingRequest);
        // Force-initialize lazy collections before leaving transaction
        saved.getSubmissionList().size();
        saved.getQuestions().size();
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Request> getAllRequests() {
        return requestRepository.findAllWithDetails();
    }

    public List<Request> getRequestsByType(RequestType requestType) {
        return requestRepository.findByRequestType(requestType);
    }


    @Transactional(readOnly = true)
    public Request getRequestById(Long id) {
        Request request = requestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        return request;
    }

    public List<Request> getRequestsByUserId(Long userId) {
        return requestRepository.findByUserIdUser(userId);
    }


    @Transactional
    public Request approveRequest(Long requestId, Status status) {
        if (status != Status.APPROVED && status != Status.REJECTED) {
            throw new IllegalArgumentException("Invalid status. Use APPROVED or REJECTED.");
        }

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        Status oldStatus = request.getStatus();
        request.setStatus(status);
        Request savedRequest = requestRepository.save(request);

        // Log the approval/rejection
        logsService.logRequestApproval(savedRequest, request.getUser(), 
                                     status == Status.APPROVED, 
                                     "Request " + (status == Status.APPROVED ? "approved" : "rejected"), 
                                     "127.0.0.1", "System");

        savedRequest.getSubmissionList().size();
        savedRequest.getQuestions().size();
        return savedRequest;
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
        User oldAgent = request.getAgent();
        request.setAgent(agent);

        // Change the status to "assigned"
        request.setStatus(Status.ASSIGNED);

        // Save the updated request
        Request savedRequest = requestRepository.save(request);

        // Log the agent assignment
        logsService.logAgentAssignment(savedRequest, request.getUser(), 
                                      oldAgent, agent, 
                                      "127.0.0.1", "System", 
                                      "Agent assigned to request");

        savedRequest.getSubmissionList().size();
        savedRequest.getQuestions().size();
        return savedRequest;
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);  // Fetch users based on role
    }

    @Transactional(readOnly = true)
    public List<Request> getRequestsAssignedToAgent(Long agentId) {
        return requestRepository.findByAgent_IdUser(agentId);
    }

    @Transactional
    public Request updateRequestStatus(Long requestId, Status newStatus) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        Status oldStatus = request.getStatus();
        request.setStatus(newStatus);
        Request savedRequest = requestRepository.save(request);

        // Log the status change
        logsService.logStatusChange(savedRequest, request.getUser(), 
                                   oldStatus, newStatus, 
                                   "127.0.0.1", "System", 
                                   "Status updated");

        savedRequest.getSubmissionList().size();
        savedRequest.getQuestions().size();
        return savedRequest;
    }

    @Transactional
    public void deleteRequest(Long id) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // Delete request-contact-status records
        requestContactStatusService.deleteByRequestIdR(request.getIdR());

        // Delete submissions and their responses using SubmissionService
        submissionService.deleteSubmissionsByRequestId(request.getIdR());

        // Delete associated report if any
        if (request.getReport() != null) {
            request.setReport(null);
            reportRepository.delete(reportRepository.findByRequest(request).orElse(null));
        }

        // Break association with questions
        request.getQuestions().clear();
        requestRepository.save(request);

        // Now delete the request
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


    @Transactional
    public Request updateNote(Long id, String note) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        request.setNote(note);
        Request savedRequest = requestRepository.save(request);

        // Log the note update
        logsService.logRequestUpdate(savedRequest, request.getUser(), 
                                    "Note updated: " + note, 
                                    "127.0.0.1", "System");

        savedRequest.getSubmissionList().size();
        savedRequest.getQuestions().size();
        return savedRequest;
    }

    public List<Question> getQuestionsByCategoryAndType(CategoryRequest category, QuestionType questionType) {
        if (category != null && questionType != null) {
            return questionRepository.findByCategoryRequestAndQuestionType(category, questionType);
        } else if (category != null) {
            return questionRepository.findByCategoryRequest(category);
        } else if (questionType != null) {
            return questionRepository.findByQuestionType(questionType);
        } else {
            return questionRepository.findAllDistinct();
        }
    }
}







