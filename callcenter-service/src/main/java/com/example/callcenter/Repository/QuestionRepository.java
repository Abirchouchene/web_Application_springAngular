package com.example.callcenter.Repository;

import com.example.callcenter.Entity.CategoryRequest;
import com.example.callcenter.Entity.Question;
import com.example.callcenter.Entity.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    @Query("SELECT DISTINCT q FROM Question q JOIN q.requests r WHERE r.categoryRequest = :category")
    List<Question> findByCategoryRequest(@Param("category") CategoryRequest category);
    
    List<Question> findByQuestionType(QuestionType questionType);
    
    @Query("SELECT DISTINCT q FROM Question q JOIN q.requests r WHERE r.categoryRequest = :category AND q.questionType = :questionType")
    List<Question> findByCategoryRequestAndQuestionType(@Param("category") CategoryRequest category, @Param("questionType") QuestionType questionType);
    
    @Query("SELECT DISTINCT q FROM Question q")
    List<Question> findAllDistinct();
}