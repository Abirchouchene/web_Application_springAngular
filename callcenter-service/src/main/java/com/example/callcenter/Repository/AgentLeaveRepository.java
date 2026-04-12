package com.example.callcenter.Repository;

import com.example.callcenter.Entity.AgentLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AgentLeaveRepository extends JpaRepository<AgentLeave, Long> {
   /* @Query("SELECT al.agent.idUser FROM AgentLeave al " +
            "WHERE :today BETWEEN al.startDate AND al.endDate")
    List<Long> findAgentsOnLeave(LocalDate today);*/

    @Query("SELECT al FROM AgentLeave al WHERE :selectedDate BETWEEN al.startDate AND al.endDate")
    List<AgentLeave> findByDate(@Param("selectedDate") LocalDate selectedDate);

}