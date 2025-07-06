package com.example.callcenter.Entity;

import com.example.callcenter.Entity.ReportStatus;
import com.example.callcenter.Entity.Request;
import com.example.callcenter.Entity.RequestType;
import com.example.callcenter.Entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Report implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private Request request;

    private String requestTitle;

    @Enumerated(EnumType.STRING)
    private RequestType requestType;



    private LocalDateTime generatedDate;

    @Enumerated(EnumType.STRING)
    private ReportStatus status;



    private LocalDateTime approvedDate;

    private LocalDateTime sentDate;

    private Integer totalContacts;

    private Integer contactedContacts;

    private Double contactRate;
    @Column(name = "statistics_data", columnDefinition = "LONGTEXT")

    private String statisticsData; // JSON string to store question responses and other statistics
}