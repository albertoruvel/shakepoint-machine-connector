package com.shakepoint.web.io.service.impl;

import com.github.roar109.syring.annotation.ApplicationProperty;
import com.shakepoint.web.io.data.dto.res.MachineConnectionStatusResponse;
import com.shakepoint.web.io.data.entity.Machine;
import com.shakepoint.web.io.data.entity.MachineConnection;
import com.shakepoint.web.io.data.repository.MachineConnectionRepository;
import com.shakepoint.web.io.netty.ConnectionInitializer;
import com.shakepoint.web.io.service.ConnectorService;
import com.shakepoint.web.io.service.EmailService;
import com.shakepoint.web.io.service.AWSS3Service;
import com.shakepoint.web.io.util.TransformationUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.*;

@Startup
@Singleton
public class ConnectorServiceImpl implements ConnectorService {

    @Inject
    private MachineConnectionRepository repository;

    @Inject
    private AWSS3Service AWSS3Service;

    @Inject
    private EmailService emailService;

    @Inject
    @ApplicationProperty(name = "com.shakepoint.web.io.port.range", type = ApplicationProperty.Types.SYSTEM)
    private String portRange;

    @Inject
    @ApplicationProperty(name = "com.shakepoint.web.io.machine.max.prepurchases", type = ApplicationProperty.Types.SYSTEM)
    private String maxPrepurchasesPerMachine;

    @Inject
    @ApplicationProperty(name = "com.shakepoint.web.io.maxBufferUsage", type = ApplicationProperty.Types.SYSTEM)
    private String maxBufferUsage;


    private final Logger log = Logger.getLogger(getClass());
    private final List<ChannelFuture> openConnections = Collections.synchronizedList(new ArrayList());

    @PostConstruct
    public void init() {
        log.info("Getting all connections data");
        final List<Machine> machines = repository.getMachines();
        createConnections(machines);
    }

    @PreDestroy
    public void destroy() {
        try {
            log.info("Closing existing connections...");
            for (ChannelFuture channel : openConnections) {
                channel.channel().close();
            }
            repository.closeAllConnections();
        } catch (Exception ex) {
            log.error("Could not close connection", ex);
        }
    }

    @Override
    public void createConnection(String machineId)throws InterruptedException{
        NioEventLoopGroup acceptorGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup handlerGroup = new NioEventLoopGroup(1);
        startConnection(acceptorGroup, handlerGroup, machineId);
    }


    private void createConnections(List<Machine> machines) {
        try {
            for (Machine machine : machines) {
                createConnection(machine.getId());
            }
        } catch (InterruptedException ex) {
            log.error("Interrupted while creating netty bootstrap", ex);
        } catch (Exception ex) {
            log.error("Unexpected error", ex);
        }
    }

    private int getFromPort() {
        return Integer.parseInt(portRange.split("-")[0]);
    }

    private int getToPort() {
        return Integer.parseInt(portRange.split("-")[1]);
    }

    @Override
    public Response getMachineConnectionStatus(String connectionId) throws Exception {
        MachineConnection connection = repository.getConnectionById(connectionId);
        if (connection == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok(new MachineConnectionStatusResponse(connection.isConnectionActive(), connection.getPort())).build();
        }

    }

    private ChannelFuture startConnection(NioEventLoopGroup acceptorGroup, NioEventLoopGroup handlerGroup, String machineId) throws InterruptedException {
        MachineConnection connection = repository.getConnection(machineId);
        if (connection == null) {
            //create a new one
            connection = TransformationUtil.createMachineConnection(machineId, getFromPort(), getToPort());
            while (!repository.isPortAvailable(connection.getPort())) {
                connection = TransformationUtil.createMachineConnection(machineId, getFromPort(), getToPort());
            }
            repository.createConnection(connection);
        }
        ServerBootstrap bootstrap = new ServerBootstrap();
        log.info(String.format("Creating connection socket for %s", machineId));
        bootstrap.group(acceptorGroup, handlerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ConnectionInitializer(machineId, repository, Integer.parseInt(maxPrepurchasesPerMachine), AWSS3Service, emailService, Integer.parseInt(maxBufferUsage)))
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_BACKLOG, 5)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.SO_BACKLOG, 5);
        ChannelFuture channel = bootstrap.localAddress(connection.getPort()).bind().sync();
        openConnections.add(channel);
        log.info(String.format("Started Netty server on port %d", connection.getPort()));
        return channel;
    }
}
