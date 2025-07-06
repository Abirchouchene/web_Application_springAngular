package com.example.callcenter.Service;

import com.example.callcenter.Entity.Response;
import com.example.callcenter.Entity.Submission;
import com.example.callcenter.Repository.ResponseRepository;
import com.example.callcenter.Repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionService {
    
    private final SubmissionRepository submissionRepository;
    private final ResponseRepository responseRepository;

    public List<Submission> findByRequestId(Long requestId) {
        return submissionRepository.findByRequestIdR(requestId);
    }

    public List<Submission> findByContactId(Long contactId) {
        return submissionRepository.findByContactId(contactId);
    }

    public Submission findById(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
    }

    @Transactional
    public void deleteSubmissionById(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
        
        // Delete responses for this submission first
        responseRepository.deleteBySubmission(submission);
        
        // Delete the submission
        submissionRepository.delete(submission);
    }

    @Transactional
    public void deleteSubmissionsByContactId(Long contactId) {
        // Find all submissions for this contact
        List<Submission> submissions = submissionRepository.findByContactId(contactId);
        
        // Delete responses for each submission first
        for (Submission submission : submissions) {
            responseRepository.deleteBySubmission(submission);
        }
        
        // Delete the submissions
        submissionRepository.deleteAll(submissions);
    }

    @Transactional
    public void deleteSubmissionsByRequestId(Long requestId) {
        // Find all submissions for this request
        List<Submission> submissions = submissionRepository.findByRequestIdR(requestId);
        
        // Delete responses for each submission first
        for (Submission submission : submissions) {
            responseRepository.deleteBySubmission(submission);
        }
        
        // Delete the submissions
        submissionRepository.deleteAll(submissions);
    }

    @Transactional
    public void deleteResponsesBySubmission(Submission submission) {
        responseRepository.deleteBySubmission(submission);
    }

    public List<Response> getResponsesBySubmissionId(Long submissionId) {
        Submission submission = findById(submissionId);
        return responseRepository.findBySubmission(submission);
    }
} 