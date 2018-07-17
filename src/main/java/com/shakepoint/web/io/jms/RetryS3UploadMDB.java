package com.shakepoint.web.io.jms;

import com.shakepoint.web.io.service.AWSS3Service;
import org.apache.log4j.Logger;

import javax.ejb.*;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@TransactionManagement(TransactionManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@MessageDriven(name = "RetryS3UploadMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "retry_file_upload"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")})
public class RetryS3UploadMDB implements MessageListener {

    @Inject
    private AWSS3Service s3Service;

    private final Logger log = Logger.getLogger(getClass());

    @Override
    public void onMessage(Message message) {
        try {
            TextMessage textMessage = (TextMessage) message;
            s3Service.retryUpload(textMessage.getText());
        } catch (Exception ex) {
            log.error("Could not process upload retry", ex);
        }
    }
}
