package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Request;
import com.example.callcenter.Entity.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByRequestType(RequestType requestType);
    List<Request> findByAgent_IdUser(Long agentId);
    List<Request> findByUserIdUser(Long userId);

}
