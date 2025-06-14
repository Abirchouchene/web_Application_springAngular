package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Contact;
import com.example.callcenter.Entity.Question;
import com.example.callcenter.Entity.Response;
import com.example.callcenter.Entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResponseRepository extends JpaRepository<Response, Long> {

    Optional<Response> findByQuestionAndSubmission(Question question, Submission submission);

}