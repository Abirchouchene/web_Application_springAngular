package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Question;
import com.example.callcenter.Entity.Request;
import com.example.callcenter.Entity.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByRequestType(RequestType requestType);
    List<Request> findByAgent_IdUser(Long agentId);
    List<Request> findByUserIdUser(Long userId);
    @Query("SELECT DISTINCT q FROM Request r JOIN r.questions q WHERE r.categoryRequest = 'RECLAMATION'")
    List<Question> findQuestionsByReclamationRequests();


}
