package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}