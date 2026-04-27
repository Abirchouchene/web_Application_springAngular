package com.example.callcenter.Service;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.resource.Emailv31;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class EmailService {

    @Value("${mailjet.api-key}")
    private String apiKey;

    @Value("${mailjet.secret-key}")
    private String secretKey;

    @Value("${mailjet.sender-email}")
    private String senderEmail;

    @Value("${mailjet.sender-name}")
    private String senderName;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("classpath:templates/email/reset-password.html")
    private Resource resetPasswordTemplateResource;

    @Value("classpath:templates/email/welcome.html")
    private Resource welcomeTemplateResource;

    @Value("classpath:templates/email/report-approval.html")
    private Resource reportApprovalTemplateResource;

    @Value("classpath:templates/email/stats-section.html")
    private Resource statsSectionTemplateResource;

    private String resetPasswordTemplate;
    private String welcomeTemplate;
    private String reportApprovalTemplate;
    private String statsSectionTemplate;

    @PostConstruct
    void loadEmailTemplates() {
        try {
            resetPasswordTemplate  = new String(FileCopyUtils.copyToByteArray(resetPasswordTemplateResource.getInputStream()),  StandardCharsets.UTF_8);
            welcomeTemplate        = new String(FileCopyUtils.copyToByteArray(welcomeTemplateResource.getInputStream()),        StandardCharsets.UTF_8);
            reportApprovalTemplate = new String(FileCopyUtils.copyToByteArray(reportApprovalTemplateResource.getInputStream()), StandardCharsets.UTF_8);
            statsSectionTemplate   = new String(FileCopyUtils.copyToByteArray(statsSectionTemplateResource.getInputStream()),   StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to load email templates", e);
            resetPasswordTemplate  = "";
            welcomeTemplate        = "";
            reportApprovalTemplate = "";
            statsSectionTemplate   = "";
        }
    }

    public void sendResetPasswordEmail(String toEmail, String toName, String resetLink) {
        try {
            MailjetClient client = new MailjetClient(
                    ClientOptions.builder().apiKey(apiKey).apiSecretKey(secretKey).build());

            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray()
                            .put(new JSONObject()
                                    .put(Emailv31.Message.FROM, new JSONObject()
                                            .put("Email", senderEmail)
                                            .put("Name", senderName))
                                    .put(Emailv31.Message.TO, new JSONArray()
                                            .put(new JSONObject()
                                                    .put("Email", toEmail)
                                                    .put("Name", toName != null ? toName : toEmail)))
                                    .put(Emailv31.Message.SUBJECT, "Réinitialisation de votre mot de passe - CallFlow")
                                    .put(Emailv31.Message.HTMLPART,
                                            buildResetPasswordHtml(toName, resetLink))
                                    .put(Emailv31.Message.TEXTPART,
                                            "Bonjour " + (toName != null ? toName : "") +
                                                    ",\n\nCliquez sur ce lien pour réinitialiser votre mot de passe : " +
                                                    resetLink + "\n\nCe lien expire dans 30 minutes.\n\nCallFlow")
                            ));

            MailjetResponse response = client.post(request);
            if (response.getStatus() == 200) {
                log.info("Reset password email sent to {}", toEmail);
            } else {
                log.error("Mailjet error {}: {}", response.getStatus(), response.getData());
            }
        } catch (Exception e) {
            log.error("Failed to send reset password email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Échec de l'envoi de l'email: " + e.getMessage());
        }
    }

    public void sendUserCreatedEmail(String toEmail, String toName, String username, String tempPassword) {
        try {
            MailjetClient client = new MailjetClient(
                    ClientOptions.builder().apiKey(apiKey).apiSecretKey(secretKey).build());

            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray()
                            .put(new JSONObject()
                                    .put(Emailv31.Message.FROM, new JSONObject()
                                            .put("Email", senderEmail)
                                            .put("Name", senderName))
                                    .put(Emailv31.Message.TO, new JSONArray()
                                            .put(new JSONObject()
                                                    .put("Email", toEmail)
                                                    .put("Name", toName != null ? toName : toEmail)))
                                    .put(Emailv31.Message.SUBJECT, "Bienvenue sur CallFlow - Votre compte a été créé")
                                    .put(Emailv31.Message.HTMLPART,
                                            buildWelcomeHtml(toName, username, tempPassword))
                                    .put(Emailv31.Message.TEXTPART,
                                            "Bonjour " + (toName != null ? toName : "") +
                                                    ",\n\nVotre compte CallFlow a été créé.\n" +
                                                    "Nom d'utilisateur: " + username + "\n" +
                                                    "Mot de passe temporaire: " + tempPassword + "\n\n" +
                                                    "Veuillez changer votre mot de passe après la première connexion.\n\nCallFlow")
                            ));

            MailjetResponse response = client.post(request);
            if (response.getStatus() == 200) {
                log.info("Welcome email sent to {}", toEmail);
            } else {
                log.error("Mailjet error {}: {}", response.getStatus(), response.getData());
            }
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildResetPasswordHtml(String name, String resetLink) {
        return String.format(resetPasswordTemplate, name != null ? name : "", resetLink);
    }

    private String buildWelcomeHtml(String name, String username, String tempPassword) {
        return String.format(welcomeTemplate, name != null ? name : "", username, tempPassword);
    }

    public void sendReportApprovalEmail(String toEmail, String toName, String requestTitle, Long reportId) {
        sendReportApprovalEmail(toEmail, toName, requestTitle, reportId, null, null, null, null);
    }

    public void sendReportApprovalEmail(String toEmail, String toName, String requestTitle, Long reportId,
                                        String aiInsightsJson, Integer totalContacts,
                                        Integer contactedContacts, Double contactRate) {
        try {
            MailjetClient client = new MailjetClient(
                    ClientOptions.builder().apiKey(apiKey).apiSecretKey(secretKey).build());

            String reportUrl  = frontendUrl + "/apps/reports/details/" + reportId;
            String pdfUrl     = frontendUrl + "/api/reports/" + reportId + "/pdf";
            String htmlBody   = buildReportApprovalHtml(toName, requestTitle, reportId,
                                    reportUrl, pdfUrl, aiInsightsJson,
                                    totalContacts, contactedContacts, contactRate);

            log.info("Sending approval email: from={} to={} reportId={}", senderEmail, toEmail, reportId);

            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray()
                            .put(new JSONObject()
                                    .put(Emailv31.Message.FROM, new JSONObject()
                                            .put("Email", senderEmail)
                                            .put("Name", senderName))
                                    .put(Emailv31.Message.TO, new JSONArray()
                                            .put(new JSONObject()
                                                    .put("Email", toEmail)
                                                    .put("Name", toName != null ? toName : toEmail)))
                                    .put(Emailv31.Message.SUBJECT, "✅ Rapport approuvé : " + requestTitle)
                                    .put(Emailv31.Message.HTMLPART, htmlBody)
                                    .put(Emailv31.Message.TEXTPART,
                                            "Bonjour " + (toName != null ? toName : "") +
                                            ",\n\nVotre demande \"" + requestTitle + "\" a été approuvée.\n\n" +
                                            "Voir le rapport : " + reportUrl + "\n" +
                                            "Télécharger le PDF : " + pdfUrl + "\n\nCallCenter")
                            ));

            MailjetResponse response = client.post(request);
            if (response.getStatus() == 200) {
                log.info("Report approval email sent successfully to {}", toEmail);
            } else {
                String errorDetail = response.getData() != null ? response.getData().toString() : "no body";
                log.error("Mailjet rejected email: status={} body={}", response.getStatus(), errorDetail);
                throw new RuntimeException("Mailjet error " + response.getStatus() + ": " + errorDetail);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to send report approval email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Échec de l'envoi d'email: " + e.getMessage(), e);
        }
    }

    private String buildReportApprovalHtml(String name, String requestTitle, Long reportId,
                                           String reportUrl, String pdfUrl, String aiInsightsJson,
                                           Integer totalContacts, Integer contactedContacts, Double contactRate) {
        String statsSection = buildStatsSection(totalContacts, contactedContacts, contactRate);
        String aiSection    = buildAiInsightsSection(aiInsightsJson);
        String safeTitle    = escapeHtml(requestTitle != null ? requestTitle : "Rapport");
        String safeName     = escapeHtml(name != null ? name : "");

        return String.format(reportApprovalTemplate,
                safeName, safeTitle,
                statsSection,
                aiSection,
                reportUrl, pdfUrl,
                reportUrl, reportUrl,
                pdfUrl, pdfUrl
        );
    }

    private String buildStatsSection(Integer totalContacts, Integer contactedContacts, Double contactRate) {
        if (totalContacts == null) return "";
        int contacted = contactedContacts != null ? contactedContacts : 0;
        double rate   = contactRate != null ? contactRate : 0.0;
        String rateColor = rate >= 70 ? "#059669" : rate >= 40 ? "#d97706" : "#dc2626";

        return String.format(statsSectionTemplate, totalContacts, contacted, rateColor, rate);
    }

    private String buildAiInsightsSection(String aiInsightsJson) {
        if (aiInsightsJson == null || aiInsightsJson.isBlank()) {
            return "<tr><td style=\"padding:0 32px 8px;\">"
                 + "<p style=\"color:#374151;line-height:1.6;margin:0;\">Le rapport d&eacute;taill&eacute; est disponible en ligne."
                 + " Cliquez sur les boutons ci-dessous pour le consulter ou le t&eacute;l&eacute;charger.</p>"
                 + "</td></tr>";
        }
        try {
            JSONObject insights = new JSONObject(aiInsightsJson);
            StringBuilder sb = new StringBuilder();
            sb.append("<tr><td style=\"padding:0 32px 8px;\">")
              .append("<div style=\"background:#faf5ff;border-radius:12px;padding:20px 24px;border:1px solid #e9d5ff;\">")
              .append("<p style=\"margin:0 0 12px;color:#6d28d9;font-weight:700;font-size:13px;")
              .append("text-transform:uppercase;letter-spacing:0.8px;\">&#x1F916; Analyse IA</p>");

            String summary = insights.optString("summary", "");
            if (!summary.isBlank()) {
                sb.append("<p style=\"margin:0 0 12px;color:#1f2937;font-size:14px;line-height:1.7;\">")
                  .append(escapeHtml(truncate(summary, 500)))
                  .append("</p>");
            }

            // Key findings (first 2)
            org.json.JSONArray findings = insights.optJSONArray("keyFindings");
            if (findings != null && findings.length() > 0) {
                sb.append("<p style=\"margin:0 0 6px;color:#4c1d95;font-weight:600;font-size:13px;\">Observations clés :</p>");
                sb.append("<ul style=\"margin:0 0 12px;padding-left:18px;color:#374151;font-size:13px;line-height:1.7;\">");
                int limit = Math.min(findings.length(), 2);
                for (int i = 0; i < limit; i++) {
                    org.json.JSONObject f = findings.optJSONObject(i);
                    if (f != null) {
                        String finding = f.optString("finding", "");
                        if (!finding.isBlank()) {
                            sb.append("<li>").append(escapeHtml(truncate(finding, 150))).append("</li>");
                        }
                    }
                }
                sb.append("</ul>");
            }

            // Sentiment
            JSONObject sentiment = insights.optJSONObject("sentimentAnalysis");
            if (sentiment != null) {
                String overall = sentiment.optString("overallSentiment", "");
                double pos = sentiment.optDouble("positivePercent", 0);
                double neg = sentiment.optDouble("negativePercent", 0);
                if (!overall.isBlank()) {
                    sb.append("<div style=\"display:inline-block;background:#ede9fe;border-radius:20px;padding:4px 12px;\">")
                      .append("<span style=\"font-size:12px;color:#5b21b6;font-weight:600;\">Sentiment : ")
                      .append(escapeHtml(overall))
                      .append(String.format(" · 😊 %.0f%% · 😞 %.0f%%", pos, neg))
                      .append("</span></div>");
                }
            }

            sb.append("</div></td></tr>");
            return sb.toString();
        } catch (Exception e) {
            log.warn("Could not parse AI insights JSON for email: {}", e.getMessage());
            return "<tr><td style=\"padding:0 32px 8px;\">"
                 + "<p style=\"color:#374151;line-height:1.6;margin:0;\">Le rapport est disponible en ligne.</p>"
                 + "</td></tr>";
        }
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "…";
    }
}
