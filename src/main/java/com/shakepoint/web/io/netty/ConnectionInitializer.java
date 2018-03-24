package com.shakepoint.web.io.netty;

import com.shakepoint.web.io.data.repository.MachineConnectionRepository;
import com.shakepoint.web.io.service.EmailService;
import com.shakepoint.web.io.service.AWSS3Service;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.apache.log4j.Logger;

public class ConnectionInitializer extends ChannelInitializer<SocketChannel> {

    private final String connectionId;
    private final MachineConnectionRepository repository;
    private final int maxPrePurchases;
    private final AWSS3Service AWSS3Service;
    private final EmailService emailService;
    private Logger log = Logger.getLogger(getClass());
    private int maxBufferUsage;

    public ConnectionInitializer(String connectionId, MachineConnectionRepository repository, int maxPrePurchases, AWSS3Service service,
                                 EmailService emailService, int maxBufferUsage) {
        this.connectionId = connectionId;
        this.repository = repository;
        this.maxPrePurchases = maxPrePurchases;
        this.AWSS3Service = service;
        this.emailService = emailService;
        this.maxBufferUsage = maxBufferUsage;
    }

    protected void initChannel(SocketChannel socketChannel) throws Exception {
        log.info(String.format("Initializing channel for connection %s", connectionId));
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast(LineBasedFrameDecoder.class.getName(), new LineBasedFrameDecoder(maxBufferUsage));
        pipeline.addLast(StringDecoder.class.getName(), new StringDecoder(CharsetUtil.UTF_8));
        pipeline.addLast(StringEncoder.class.getName(), new StringEncoder(CharsetUtil.UTF_8));
        pipeline.addLast("handler", new ChannelInboundHandler(connectionId, repository, maxPrePurchases, AWSS3Service, emailService));
        log.info(String.format("Setting connection %s to ACTIVE", connectionId));
        repository.updateMachineConnectionStatus(connectionId, true);
    }
}
