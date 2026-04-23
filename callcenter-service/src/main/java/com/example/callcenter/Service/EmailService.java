package com.example.callcenter.Service;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.resource.Emailv31;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                  <div style="background: #1976d2; color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center;">
                    <h1 style="margin: 0;">CallFlow</h1>
                  </div>
                  <div style="background: #f5f5f5; padding: 30px; border-radius: 0 0 8px 8px;">
                    <h2>Réinitialisation du mot de passe</h2>
                    <p>Bonjour %s,</p>
                    <p>Vous avez demandé la réinitialisation de votre mot de passe. Cliquez sur le bouton ci-dessous :</p>
                    <div style="text-align: center; margin: 30px 0;">
                      <a href="%s" style="background: #1976d2; color: white; padding: 14px 28px; text-decoration: none; border-radius: 6px; font-size: 16px;">
                        Réinitialiser mon mot de passe
                      </a>
                    </div>
                    <p style="color: #666; font-size: 13px;">Ce lien expire dans <strong>30 minutes</strong>.</p>
                    <p style="color: #666; font-size: 13px;">Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.</p>
                    <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
                    <p style="color: #999; font-size: 12px; text-align: center;">CallFlow - Centre d'Appels</p>
                  </div>
                </body>
                </html>
                """.formatted(name != null ? name : "", resetLink);
    }

    private String buildWelcomeHtml(String name, String username, String tempPassword) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                  <div style="background: #1976d2; color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center;">
                    <h1 style="margin: 0;">CallFlow</h1>
                  </div>
                  <div style="background: #f5f5f5; padding: 30px; border-radius: 0 0 8px 8px;">
                    <h2>Bienvenue sur CallFlow !</h2>
                    <p>Bonjour %s,</p>
                    <p>Votre compte a été créé avec succès. Voici vos identifiants :</p>
                    <div style="background: white; padding: 20px; border-radius: 6px; margin: 20px 0;">
                      <p><strong>Nom d'utilisateur :</strong> %s</p>
                      <p><strong>Mot de passe temporaire :</strong> %s</p>
                    </div>
                    <p style="color: #e53935; font-weight: bold;">Veuillez changer votre mot de passe après la première connexion.</p>
                    <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
                    <p style="color: #999; font-size: 12px; text-align: center;">CallFlow - Centre d'Appels</p>
                  </div>
                </body>
                </html>
                """.formatted(
                name != null ? name : "",
                username,
                tempPassword
        );
    }

    public void sendReportApprovalEmail(String toEmail, String toName, String requestTitle, Long reportId) {
        sendReportApprovalEmail(toEmail, toName, requestTitle, reportId, null);
    }

    public void sendReportApprovalEmail(String toEmail, String toName, String requestTitle, Long reportId, String aiInsightsJson) {
        try {
            MailjetClient client = new MailjetClient(
                    ClientOptions.builder().apiKey(apiKey).apiSecretKey(secretKey).build());

            String reportUrl = frontendUrl + "/apps/reports/details/" + reportId;
            String htmlBody = buildReportApprovalHtml(toName, requestTitle, reportId, reportUrl, aiInsightsJson);

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
                                    .put(Emailv31.Message.SUBJECT, requestTitle + " (approuvé)")
                                    .put(Emailv31.Message.HTMLPART, htmlBody)
                                    .put(Emailv31.Message.TEXTPART,
                                            "Bonjour " + (toName != null ? toName : "") +
                                                    ",\n\nVotre demande \"" + requestTitle +
                                                    "\" a été approuvée et le rapport est disponible.\n\n" +
                                                    "Voir le rapport : " + reportUrl + "\n\nCallCenter")
                            ));

            MailjetResponse response = client.post(request);
            if (response.getStatus() == 200) {
                log.info("Report approval email sent to {}", toEmail);
            } else {
                log.error("Mailjet error {}: {}", response.getStatus(), response.getData());
            }
        } catch (Exception e) {
            log.error("Failed to send report approval email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildReportApprovalHtml(String name, String requestTitle, Long reportId, String reportUrl, String aiInsightsJson) {
        String aiSection = buildAiInsightsSection(aiInsightsJson);
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="margin:0; padding:0; background:#f4f5f7; font-family: 'Segoe UI', Arial, sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#f4f5f7; padding:32px 16px;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0" border="0" style="max-width:600px; background:#ffffff; border-radius:12px; overflow:hidden; box-shadow:0 4px 12px rgba(0,0,0,0.08);">
                        <!-- Purple gradient header -->
                        <tr>
                          <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding:36px 24px; text-align:center;">
                            <div style="font-size:32px; margin-bottom:8px;">%s</div>
                            <h1 style="color:#ffffff; font-size:24px; margin:0; font-weight:600;">Rapport Approuvé</h1>
                            <p style="color:#e0e7ff; margin:8px 0 0; font-size:14px;">Votre demande a été traitée avec succès</p>
                          </td>
                        </tr>
                        <!-- Body -->
                        <tr>
                          <td style="padding:32px 32px 16px;">
                            <h2 style="color:#1f2937; font-size:20px; margin:0 0 16px;">📊 %s (approuvé)</h2>
                            <p style="color:#374151; line-height:1.6; margin:0 0 16px;">Bonjour <strong>%s</strong>,</p>
                            %s
                          </td>
                        </tr>
                        <!-- Button -->
                        <tr>
                          <td align="center" style="padding:8px 32px 24px;">
                            <a href="%s" style="display:inline-block; background:linear-gradient(135deg,#667eea 0%%,#764ba2 100%%); color:#ffffff; text-decoration:none; padding:14px 32px; border-radius:999px; font-weight:600; font-size:15px; box-shadow:0 4px 10px rgba(102,126,234,0.3);">
                              👁  Voir le Rapport
                            </a>
                          </td>
                        </tr>
                        <!-- Fallback link -->
                        <tr>
                          <td style="padding:0 32px 24px;">
                            <p style="color:#6b7280; font-size:13px; margin:0 0 4px;">Si le bouton ne fonctionne pas, copiez et collez ce lien dans votre navigateur :</p>
                            <p style="margin:0;"><a href="%s" style="color:#667eea; word-break:break-all; font-size:13px;">%s</a></p>
                          </td>
                        </tr>
                        <!-- Footer -->
                        <tr>
                          <td style="background:#f9fafb; padding:20px 32px; text-align:center; border-top:1px solid #e5e7eb;">
                            <p style="margin:0 0 4px; color:#667eea; font-weight:700; font-size:15px;">CallCenter</p>
                            <p style="margin:0; color:#9ca3af; font-size:12px;">Ce message a été envoyé automatiquement. Merci de ne pas y répondre.</p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                "✅",
                requestTitle != null ? requestTitle : "Rapport",
                name != null ? name : "",
                aiSection,
                reportUrl,
                reportUrl,
                reportUrl
        );
    }

    private String buildAiInsightsSection(String aiInsightsJson) {
        if (aiInsightsJson == null || aiInsightsJson.isBlank()) {
            return "<p style=\"color:#374151; line-height:1.6; margin:0;\">Votre rapport a été approuvé et est maintenant disponible au téléchargement.</p>";
        }
        try {
            JSONObject insights = new JSONObject(aiInsightsJson);
            StringBuilder sb = new StringBuilder();
            sb.append("<div style=\"background:#f3f4f6; border-left:4px solid #667eea; border-radius:6px; padding:16px 20px; margin:16px 0;\">");
            sb.append("<p style=\"margin:0 0 10px; color:#4c1d95; font-weight:700; font-size:13px; letter-spacing:0.5px;\">=== ANALYSE IA ===</p>");

            String summary = insights.optString("summary", "");
            if (!summary.isBlank()) {
                sb.append("<p style=\"margin:0 0 8px; color:#374151; font-weight:600; font-size:14px;\">Insights générés par l'intelligence artificielle</p>");
                sb.append("<p style=\"margin:0; color:#4b5563; line-height:1.6; font-size:13px;\">")
                  .append(escapeHtml(truncate(summary, 600)))
                  .append("</p>");
            }

            JSONObject sentiment = insights.optJSONObject("sentimentAnalysis");
            if (sentiment != null) {
                String overall = sentiment.optString("overallSentiment", "");
                if (!overall.isBlank()) {
                    sb.append("<p style=\"margin:10px 0 0; color:#6b7280; font-size:12px;\"><strong>Sentiment global :</strong> ")
                      .append(escapeHtml(overall)).append("</p>");
                }
            }
            sb.append("</div>");
            return sb.toString();
        } catch (Exception e) {
            log.warn("Could not parse AI insights JSON for email: {}", e.getMessage());
            return "<p style=\"color:#374151; line-height:1.6; margin:0;\">Votre rapport a été approuvé et est maintenant disponible.</p>";
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
