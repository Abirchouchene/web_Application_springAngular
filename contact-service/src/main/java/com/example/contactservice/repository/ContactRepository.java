package com.example.contactservice.repository;

import com.example.contactservice.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    @Query("SELECT c FROM Contact c JOIN c.tags t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :tagName, '%'))")
    List<Contact> findByTagNameLike(String tagName);
} 