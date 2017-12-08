package com.shakepoint.web.io.factory;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;

import org.apache.log4j.Logger;

import com.shakepoint.integration.jms.client.handler.JmsHandler;

public class JmsFactory {

    @Resource(lookup = "java:jboss/exported/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Inject
    private Logger log;

    @Produces
    @ApplicationScoped
    public JmsHandler createJmsHandler() {
        return new JmsHandler(connectionFactory, log);
    }
}
