package com.example.callcenter.Entity;

public enum LogAction {
    REQUEST_CREATED("Demande créée"),
    REQUEST_UPDATED("Demande mise à jour"),
    STATUS_CHANGED("Statut modifié"),
    PRIORITY_CHANGED("Priorité modifiée"),
    AGENT_ASSIGNED("Agent assigné"),
    AGENT_UNASSIGNED("Agent désassigné"),
    REQUEST_DELETED("Demande supprimée"),
    COMMENT_ADDED("Commentaire ajouté"),
    ATTACHMENT_ADDED("Pièce jointe ajoutée"),
    DEADLINE_CHANGED("Date limite modifiée"),
    REQUEST_APPROVED("Demande approuvée"),
    REQUEST_REJECTED("Demande rejetée"),
    REQUEST_REOPENED("Demande rouverte"),
    REQUEST_CLOSED("Demande fermée");

    private final String description;

    LogAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 