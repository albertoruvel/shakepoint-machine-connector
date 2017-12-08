package com.shakepoint.web.io.jms;

import com.shakepoint.web.io.service.ConnectorService;
import org.apache.log4j.Logger;

import javax.ejb.*;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@TransactionManagement(TransactionManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@MessageDriven(name = "DummyMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "machine_connection"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class MachineConnectionMDB implements MessageListener {

    @Inject
    private ConnectorService connectorService;

    private final Logger log = Logger.getLogger(getClass());

    @Override
    public void onMessage(Message message) {
        TextMessage textMessage = (TextMessage)message;
        try{
            connectorService.createConnection(textMessage.getText());
        }catch(JMSException ex){
            log.error("JMSException", ex);
        }catch(InterruptedException ex){
            log.error("InterruptedException", ex);
        }
    }
}
