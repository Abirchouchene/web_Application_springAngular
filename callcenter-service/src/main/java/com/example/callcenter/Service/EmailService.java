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
                                    .put(Emailv31.Message.SUBJECT, "Rapport approuvé - " + requestTitle)
                                    .put(Emailv31.Message.HTMLPART, buildReportApprovalHtml(toName, requestTitle, reportId))
                                    .put(Emailv31.Message.TEXTPART,
                                            "Bonjour " + (toName != null ? toName : "") +
                                                    ",\n\nLe rapport pour la demande \"" + requestTitle +
                                                    "\" a été approuvé par le manager.\n\n" +
                                                    "Vous pouvez consulter les résultats dans la plateforme CallFlow.\n\nCallFlow")
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

    private String buildReportApprovalHtml(String name, String requestTitle, Long reportId) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                  <div style="background: #4caf50; color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center;">
                    <h1 style="margin: 0;">CallFlow</h1>
                    <p style="margin: 5px 0 0;">Rapport Approuvé</p>
                  </div>
                  <div style="background: #f5f5f5; padding: 30px; border-radius: 0 0 8px 8px;">
                    <h2>Votre rapport est prêt !</h2>
                    <p>Bonjour %s,</p>
                    <p>Le rapport pour la demande <strong>"%s"</strong> a été <span style="color: #4caf50; font-weight: bold;">approuvé</span> par le manager.</p>
                    <div style="background: white; padding: 20px; border-radius: 6px; margin: 20px 0; border-left: 4px solid #4caf50;">
                      <p><strong>Demande :</strong> %s</p>
                      <p><strong>Rapport N° :</strong> %d</p>
                      <p><strong>Statut :</strong> <span style="color: #4caf50;">Approuvé</span></p>
                    </div>
                    <p>Vous pouvez maintenant consulter les résultats détaillés et télécharger le PDF depuis la plateforme.</p>
                    <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
                    <p style="color: #999; font-size: 12px; text-align: center;">CallFlow - Centre d'Appels</p>
                  </div>
                </body>
                </html>
                """.formatted(
                name != null ? name : "",
                requestTitle,
                requestTitle,
                reportId
        );
    }
}
