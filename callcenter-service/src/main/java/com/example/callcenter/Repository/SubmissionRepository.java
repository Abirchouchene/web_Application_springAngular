package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Question;
import com.example.callcenter.Entity.Request;
import com.example.callcenter.Entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    void deleteByRequest(Request request);

    List<Submission> findByRequest(Request request);
    
    List<Submission> findByRequestIdR(Long requestId);

    @Query("SELECT s FROM Submission s WHERE s.contactId = :contactId AND :question MEMBER OF s.request.questions")
    Optional<Submission> findByContactIdAndQuestion(@Param("contactId") Long contactId, @Param("question") Question question);

    List<Submission> findByContactId(Long contactId);

    Optional<Submission> findByRequestIdRAndContactId(Long requestIdR, Long contactId);

    List<Submission> findAllByRequestIdRAndContactId(Long requestIdR, Long contactId);

    @Modifying
    @Query("DELETE FROM Submission s WHERE s.request.idR = :requestId")
    void deleteByRequestId(@Param("requestId") Long requestId);
}