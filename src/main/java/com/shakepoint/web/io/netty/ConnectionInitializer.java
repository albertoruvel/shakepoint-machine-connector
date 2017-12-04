package com.shakepoint.web.io.netty;

import com.shakepoint.web.io.data.repository.MachineConnectionRepository;
import com.shakepoint.web.io.service.QrCodeService;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.apache.log4j.Logger;

public class ConnectionInitializer extends ChannelInitializer<SocketChannel> {

    private final String connectionId;
    private final MachineConnectionRepository repository;
    private final int maxPrePurchases;
    private final QrCodeService qrCodeService;
    private Logger log = Logger.getLogger(getClass());

    public ConnectionInitializer(String connectionId, MachineConnectionRepository repository, int maxPrePurchases, QrCodeService service) {
        this.connectionId = connectionId;
        this.repository = repository;
        this.maxPrePurchases = maxPrePurchases;
        this.qrCodeService = service;
    }

    protected void initChannel(SocketChannel socketChannel) throws Exception {
        log.info(String.format("Initializing channel for connection %s", connectionId));
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast(LineBasedFrameDecoder.class.getName(), new LineBasedFrameDecoder(1024));
        pipeline.addLast(StringDecoder.class.getName(), new StringDecoder(CharsetUtil.UTF_8));
        pipeline.addLast(StringEncoder.class.getName(), new StringEncoder(CharsetUtil.UTF_8));
        pipeline.addLast("handler", new ChannelInboundHandler(connectionId, repository, maxPrePurchases, qrCodeService));
    }
}
