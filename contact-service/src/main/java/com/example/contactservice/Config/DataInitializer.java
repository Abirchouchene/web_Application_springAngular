package com.example.contactservice.config;

import com.example.contactservice.entity.Tag;
import com.example.contactservice.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final TagRepository tagRepository;

    private static final List<String> DEFAULT_TAGS = List.of("VIP", "SALES", "RESELLER");

    @Override
    public void run(String... args) {
        Set<String> existingNames = tagRepository.findAll().stream()
                .map(Tag::getName)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        for (String tagName : DEFAULT_TAGS) {
            if (!existingNames.contains(tagName.toUpperCase())) {
                Tag tag = new Tag();
                tag.setName(tagName);
                tagRepository.save(tag);
                log.info("Created default tag: {}", tagName);
            }
        }
    }
}
