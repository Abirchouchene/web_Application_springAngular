package com.example.callcenter.Config;

import com.example.callcenter.Entity.Priority;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PriorityConverter implements AttributeConverter<Priority, String> {

    @Override
    public String convertToDatabaseColumn(Priority priority) {
        if (priority == null) {
            return null;
        }
        return priority.name();
    }

    @Override
    public Priority convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return Priority.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            // Log the error and return a default value
            System.err.println("Invalid priority value in database: " + dbData);
            return Priority.MEDIUM; // Default fallback
        }
    }
} 