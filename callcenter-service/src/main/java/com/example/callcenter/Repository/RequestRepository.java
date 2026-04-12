package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Question;
import com.example.callcenter.Entity.Request;
import com.example.callcenter.Entity.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByRequestType(RequestType requestType);
    List<Request> findByAgent_IdUser(Long agentId);
    List<Request> findByUserIdUser(Long userId);
    @Query("SELECT DISTINCT q FROM Request r JOIN r.questions q WHERE r.categoryRequest = 'RECLAMATION'")
    List<Question> findQuestionsByReclamationRequests();

    @EntityGraph(attributePaths = {"user", "agent", "report"})
    @Query("SELECT r FROM Request r")
    List<Request> findAllWithDetails();

    @EntityGraph(attributePaths = {"user", "agent", "report"})
    @Query("SELECT r FROM Request r WHERE r.idR = :id")
    Optional<Request> findByIdWithDetails(Long id);
}
