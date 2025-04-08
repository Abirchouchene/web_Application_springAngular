package com.example.callcenter.Repository;

import com.example.callcenter.Entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findByTagsContaining(String tag);}