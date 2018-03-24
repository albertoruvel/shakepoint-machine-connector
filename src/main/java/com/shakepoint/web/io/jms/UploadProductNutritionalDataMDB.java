package com.shakepoint.web.io.jms;

import com.shakepoint.web.io.data.repository.MachineConnectionRepository;
import com.shakepoint.web.io.service.AWSS3Service;
import org.apache.log4j.Logger;

import javax.ejb.*;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@TransactionManagement(TransactionManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@MessageDriven(name = "NutritionalDataProcessor", activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "nutritional_data"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class UploadProductNutritionalDataMDB implements MessageListener{

    @Inject
    private AWSS3Service awss3Service;



    @Override
    public void onMessage(Message message) {
        TextMessage textMessage = (TextMessage)message;
        try{
            awss3Service.createProductNutritionalData(textMessage.getText());
        }catch(Exception ex){
            Logger.getLogger(getClass()).error("Could not extract text from message", ex);
        }
    }
}
