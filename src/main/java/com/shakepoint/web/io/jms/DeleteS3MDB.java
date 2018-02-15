package com.shakepoint.web.io.jms;

import com.shakepoint.web.io.service.QrCodeService;

import javax.ejb.*;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;

@TransactionManagement(TransactionManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@MessageDriven(name = "DummyMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "machine_connection"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
public class DeleteS3MDB implements MessageListener{

    @Inject
    QrCodeService qrCodeService;

    public void onMessage(Message message) {

    }
}
