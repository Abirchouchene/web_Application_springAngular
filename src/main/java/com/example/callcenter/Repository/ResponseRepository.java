package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Contact;
import com.example.callcenter.Entity.Question;
import com.example.callcenter.Entity.Response;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResponseRepository extends JpaRepository<Response, Long> {
    Optional<Response> findByQuestionAndContact(Question question, Contact contact);
}