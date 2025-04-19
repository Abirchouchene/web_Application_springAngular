package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

   /* @Query("SELECT c FROM Contact c JOIN c.tags t WHERE t.name = :tagName")
    List<Contact> findByTagName(@Param("tagName") String tagName);*/
}