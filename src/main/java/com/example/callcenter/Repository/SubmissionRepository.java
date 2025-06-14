package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Question;
import com.example.callcenter.Entity.Request;
import com.example.callcenter.Entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    void deleteByRequest(Request request);

    List<Submission> findByRequest(Request request);
    @Query("SELECT s FROM Submission s WHERE s.contact.idC = :contactId AND :question MEMBER OF s.request.questions")
    Optional<Submission> findByContactIdAndQuestion(@Param("contactId") Long contactId, @Param("question") Question question);

}