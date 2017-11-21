package com.shakepoint.web.io.netty;

import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class GsonDecoder<String> extends MessageToMessageDecoder <String>{

    private final Gson gson = new Gson();

    public GsonDecoder(){

    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, String t, List<Object> list) throws Exception {
        list.add(t);
    }



}
