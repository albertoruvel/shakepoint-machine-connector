package com.shakepoint.web.io.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;
import org.apache.log4j.Logger;

public class ConnectionInitializer extends ChannelInitializer<SocketChannel> {

    private String connectionId;
    private Logger log = Logger.getLogger(getClass());

    public ConnectionInitializer(String connectionId) {
        this.connectionId = connectionId;
    }

    protected void initChannel(SocketChannel socketChannel) throws Exception {
        log.info(String.format("Initializing channel for connection %s", connectionId));
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast(LineBasedFrameDecoder.class.getName(), new LineBasedFrameDecoder(256));
        pipeline.addLast(StringDecoder.class.getName(), new StringDecoder(CharsetUtil.UTF_8));
        pipeline.addLast(JsonObjectDecoder.class.getName(), new JsonObjectDecoder());
        pipeline.addLast("handler", new SimpleChannelInboundHandler<String>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
                log.info(String.format("Received new message for %s\n%s", connectionId, s));

            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                log.info(msg);
            }
        });

    }
}
