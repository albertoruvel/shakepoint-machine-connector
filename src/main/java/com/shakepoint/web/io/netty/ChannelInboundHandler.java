package com.shakepoint.web.io.netty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shakepoint.web.io.data.dto.req.socket.MachineMessage;
import com.shakepoint.web.io.data.dto.req.socket.MachineMessageType;
import com.shakepoint.web.io.data.dto.res.socket.PreAuthPurchase;
import com.shakepoint.web.io.data.entity.Product;
import com.shakepoint.web.io.data.entity.Purchase;
import com.shakepoint.web.io.data.entity.PurchaseStatus;
import com.shakepoint.web.io.data.repository.MachineConnectionRepository;
import com.shakepoint.web.io.service.QrCodeService;
import com.shakepoint.web.io.util.TransformationUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ChannelInboundHandler extends SimpleChannelInboundHandler<String> {

    private final String connectionId;
    private final int maxPrePurchases;
    private final MachineConnectionRepository repository;
    private final QrCodeService qrCodeService;
    private final Gson gson = new GsonBuilder().create();
    private Logger log = Logger.getLogger(getClass());

    public ChannelInboundHandler(String connectionId, MachineConnectionRepository repository,
                                 int maxPrePurchases, QrCodeService qrCodeService) {
        this.connectionId = connectionId;
        this.repository = repository;
        this.qrCodeService = qrCodeService;
        this.maxPrePurchases = maxPrePurchases;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String jsonMessage = (String) msg;
        MachineMessage request = gson.fromJson(jsonMessage, MachineMessage.class);
        processMachineMessageRequest(ctx, request);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
        log.info(s);
    }


    private void dispatchTurnOnMessage(ChannelHandlerContext cxt, MachineMessage request) {
        final String machineId = request.getMachineId();

        //check for existing purchases with status pre_auth
        List<Purchase> machinePurchases = repository.getMachinePreAuthorizedPurchases(machineId);
        List<PreAuthPurchase> preAuthPurchases = new ArrayList();
        if (machinePurchases.isEmpty()) {
            //create N purchases
            preAuthPurchases.addAll(TransformationUtil.createPreAuthPurchases(createPurchases(machineId, maxPrePurchases)));
        } else if (machinePurchases.size() < maxPrePurchases) {
            //create (maxPrePurchases - size) purchases
            preAuthPurchases.addAll(TransformationUtil.createPreAuthPurchases(
                    createPurchases(machineId, (maxPrePurchases - machinePurchases.size()))));
        }

        //create a json response
        final String json = gson.toJson(preAuthPurchases);

        log.info("Sending pre authorized codes to client " + connectionId);
        cxt.channel().writeAndFlush(json + "\n");
    }

    private List<Purchase> createPurchases(String machineId, int nPurchases) {
        //get products for machine
        List<Product> products = repository.getMachineAvailableProducts(machineId);
        List<Purchase> purchases = new ArrayList();
        Purchase purchase;
        int index;
        for (int i = 0; i < nPurchases; i++) {
            index = ThreadLocalRandom.current().nextInt(0, products.size() + 1);
            purchase = new Purchase();
            purchase.setMachineId(machineId);
            purchase.setProductId(products.get(index).getId());
            purchase.setPurchaseDate(TransformationUtil.DATE_FORMAT.format(new Date()));
            purchase.setStatus(PurchaseStatus.PRE_AUTH);
            purchase.setQrCodeUrl(qrCodeService.createQrCode(purchase));
            repository.addPurchase(purchase);
            purchases.add(purchase);
        }
        return purchases;
    }

    private void dispatchQrCodeValidation(ChannelHandlerContext cxt, MachineMessage request) {

    }

    private void dispatchNotValidMessageType(ChannelHandlerContext cxt, MachineMessage request) {

    }

    private void processMachineMessageRequest(ChannelHandlerContext cxt, MachineMessage request) {
        MachineMessageType type = MachineMessageType.get(request.getType());
        switch (type) {
            case TURN_ON:
                // get machine connection and set it active
                dispatchTurnOnMessage(cxt, request);
                break;
            case QR_CODE_DISPENSED:
                dispatchQrCodeValidation(cxt, request);
                break;
            case NOT_VALID:
                dispatchNotValidMessageType(cxt, request);
                break;
        }
    }
}
