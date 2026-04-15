package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Question;
import com.example.callcenter.Entity.Response;
import com.example.callcenter.Entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResponseRepository extends JpaRepository<Response, Long> {

    List<Response> findBySubmission(Submission submission);

    List<Response> findBySubmissionAndQuestion(Submission submission, Question question);

    @Modifying
    @Query("DELETE FROM Response r WHERE r.submission = :submission")
    void deleteBySubmission(@Param("submission") Submission submission);
    
    @Modifying
    @Query("DELETE FROM Response r WHERE r.submission.id = :submissionId")
    void deleteBySubmissionId(@Param("submissionId") Long submissionId);

}