package com.shakepoint.web.io.service.impl;

import com.github.roar109.syring.annotation.ApplicationProperty;
import com.shakepoint.web.io.data.dto.MachineConnectionResponse;
import com.shakepoint.web.io.data.dto.MachineConnectionStatusResponse;
import com.shakepoint.web.io.data.dto.NewMachineConnectionRequest;
import com.shakepoint.web.io.data.entity.MachineConnection;
import com.shakepoint.web.io.data.repository.MachineConnectionRepository;
import com.shakepoint.web.io.netty.ConnectionInitializer;
import com.shakepoint.web.io.service.ConnectorService;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Singleton
@Startup
public class ConnectorServiceImpl implements ConnectorService {


    @Inject
    private MachineConnectionRepository repository;

    @Inject
    @ApplicationProperty(name = "com.shakepoint.web.io.port.range", type = ApplicationProperty.Types.SYSTEM)
    private String portRange;

    static final DateFormat DATE_FORMAT = new SimpleDateFormat("mm/dd/yyyy hh:mm:ss a");
    private final Logger log = Logger.getLogger(getClass());
    private final List<ChannelFuture> openConnections = Collections.synchronizedList(new ArrayList());

    @PostConstruct
    public void init() {
        final List<MachineConnection> connections = repository.getConnections();
        createConnections(connections);
    }


    private void createConnections(List<MachineConnection> connections) {
        NioEventLoopGroup acceptorGroup;
        NioEventLoopGroup handlerGroup;
        ServerBootstrap bootstrap;
        try {
            for (MachineConnection connection : connections) {
                acceptorGroup = new NioEventLoopGroup(1);
                handlerGroup = new NioEventLoopGroup(1);
                startConnection(acceptorGroup, handlerGroup, connection);
                //update database
                connection.setConnectionActive(true);
                repository.updateConnection(connection);
            }
        } catch (InterruptedException ex) {
            log.error("Interrupted while creating netty bootstrap", ex);
        }
    }

    private int getFromPort() {
        return Integer.parseInt(portRange.split("-")[0]);
    }

    private int getToPort() {
        return Integer.parseInt(portRange.split("-")[1]);
    }

    @Override
    public Response createMachineConnection(NewMachineConnectionRequest request) {
        //check if there is an existing machine connection
        MachineConnection connection = repository.getConnection(request.getMachineId());
        int randomPort = ThreadLocalRandom.current().nextInt(getFromPort(), getToPort() + 1);
        if (connection == null) {
            connection = new MachineConnection();
            connection.setConnectionActive(false);
            connection.setLastUpdate(DATE_FORMAT.format(new Date()));
            connection.setMachineId(request.getMachineId());
            connection.setMachineToken(UUID.randomUUID().toString());
            connection.setPort(randomPort);
            repository.createConnection(connection);

            //try to open connection
            try {
                startConnection(new NioEventLoopGroup(1), new NioEventLoopGroup(1), connection);
                return Response.ok(new MachineConnectionResponse(randomPort, connection.getId(),
                        "Machine connection have been created, try to open connection at /io/machine/openMachineConnection"))
                        .build();
            } catch (InterruptedException ex) {
                log.error("Interrupted while creating netty bootstrap", ex);
                return Response.status(500).entity(new MachineConnectionResponse(-1, null, "Could not start connection, try again")).build();
            }

        } else {
            if (repository.isConnectionActive(connection.getId())) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new MachineConnectionResponse(-1, null, "Connection is already active")).build();
            } else {
                try {
                    startConnection(new NioEventLoopGroup(1), new NioEventLoopGroup(1), connection);
                    return Response.ok(new MachineConnectionResponse(randomPort, connection.getId(),
                            "Machine connection have been created, try to open connection at /io/machine/openMachineConnection"))
                            .build();
                } catch (InterruptedException ex) {
                    log.error("Interrupted while creating netty bootstrap", ex);
                    return Response.status(500).entity(new MachineConnectionResponse(-1, null, "Could not start connection, try again")).build();
                }
            }

        }
    }

    @Override
    public Response getMachineConnectionStatus(String connectionId) throws Exception {
        MachineConnection connection = repository.getConnection(connectionId);
        if (connection == null){
             return Response.status(Response.Status.NOT_FOUND).build();
        }else{
            return Response.ok(new MachineConnectionStatusResponse(connection.isConnectionActive(), connection.getPort())).build();
        }

    }

    private ChannelFuture startConnection(NioEventLoopGroup acceptorGroup, NioEventLoopGroup handlerGroup, MachineConnection connection) throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        log.info(String.format("Creating connection socket for %s", connection.getId()));
        bootstrap.group(acceptorGroup, handlerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ConnectionInitializer(connection.getId()))
                .option(ChannelOption.SO_BACKLOG, 5)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        ChannelFuture channel = bootstrap.localAddress(connection.getPort()).bind().sync();
        openConnections.add(channel);
        log.info(String.format("Started Netty server on port %d", connection.getPort()));
        return channel;
    }

}
