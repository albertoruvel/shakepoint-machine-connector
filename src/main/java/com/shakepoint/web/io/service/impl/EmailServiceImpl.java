package com.shakepoint.web.io.service.impl;

import com.github.roar109.syring.annotation.ApplicationProperty;
import com.shakepoint.email.EmailQueue;
import com.shakepoint.integration.jms.client.handler.JmsHandler;
import com.shakepoint.web.io.email.Email;
import com.shakepoint.web.io.service.EmailService;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.Map;

public class EmailServiceImpl implements EmailService {

    @Inject
    @ApplicationProperty(name = "com.shakepoint.web.email.enabled", type = ApplicationProperty.Types.SYSTEM, valueType = ApplicationProperty.ValueType.STRING)
    private String emailsEnabled;

    @Inject
    private Logger log;

    @Inject
    private JmsHandler jmsHandler;

    public void sendEmail(Email emailType, String to, Map<String, Object> params) {
        try {
            if (Boolean.parseBoolean(emailsEnabled)) {
                log.info("Sending email to " + to);

                jmsHandler.send(EmailQueue.NAME, new com.shakepoint.email.model.Email(to, emailType.getTemplateName(),
                        emailType.getSubject(), params)
                        .toJson());
            } else {
                log.info("Emails not enabled");
            }
        } catch (Exception e) {
            log.error("An error occurred trying to send email to "+to);
            log.error(e.getMessage(), e);
        }
    }
}
