package com.shakepoint.web.io.netty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shakepoint.web.io.data.dto.req.socket.*;
import com.shakepoint.web.io.data.dto.res.socket.PreAuthPurchase;
import com.shakepoint.web.io.data.dto.res.socket.ProductRecap;
import com.shakepoint.web.io.data.entity.*;
import com.shakepoint.web.io.data.repository.MachineConnectionRepository;
import com.shakepoint.web.io.service.QrCodeService;
import com.shakepoint.web.io.util.TransformationUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


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
        MachineMessage request = null;
        try{
            String jsonMessage = (String) msg;
            request = gson.fromJson(jsonMessage, MachineMessage.class);
        }catch(Exception ex){
            dispatchNotValidMessageType(ctx);
            return;
        }

        processMachineMessageRequest(ctx, request);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            log.info(String.format("Client %s have been disconnected", connectionId));
        } else {
            log.error("Unexpected error", cause);
            cause.printStackTrace();
        }
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
            log.info(String.format("Creating new pre authorized purchases for machine %s", machineId));
            //create N purchases
            preAuthPurchases.addAll(TransformationUtil.createPreAuthPurchases(createPurchases(machineId), repository));
        } else {
            int removedPurchases = repository.removePreAuthorizedPurchases(machineId);
            log.info(String.format("%d pre authorized purchases have been removed to create new ones", removedPurchases));
            //create (maxPrePurchases - size) purchases
            preAuthPurchases.addAll(TransformationUtil.createPreAuthPurchases(
                    createPurchases(machineId), repository));
        }

        //create a json response
        final String json = gson.toJson(preAuthPurchases);

        log.info("Sending pre authorized codes to client " + connectionId);
        cxt.channel().writeAndFlush(json + "\n");
    }

    private List<Purchase> createPurchases(String machineId) {
        //get products for machine
        List<Product> products = repository.getMachineAvailableProducts(machineId);
        log.info(String.format("Creating %d for %d products, also uploading QR images to S3 service", maxPrePurchases, products.size()));
        List<Purchase> purchases = new ArrayList();
        Purchase purchase;
        for (Product p : products) {
            for (int j = 0; j < maxPrePurchases; j++) {
                purchase = createPurchase(machineId, p.getId());
                repository.addPurchase(purchase);
                purchases.add(purchase);
            }
        }
        return purchases;
    }

    private Purchase createPurchase(String machineId, String productId){
        Purchase purchase = new Purchase();
        purchase.setMachineId(machineId);
        purchase.setProductId(productId);
        purchase.setPurchaseDate(TransformationUtil.DATE_FORMAT.format(new Date()));
        purchase.setStatus(PurchaseStatus.PRE_AUTH);
        purchase.setQrCodeUrl(qrCodeService.createQrCode(purchase));
        return purchase;
    }

    private void dispatchNotValidMessageType(ChannelHandlerContext cxt) {
        //TODO: send email to technician notifying that this should not happen
    }

    private void processMachineMessageRequest(ChannelHandlerContext cxt, MachineMessage request) {
        MachineMessageType type = MachineMessageType.get(request.getType());
        switch (type) {
            case TURN_ON:
                // get machine connection and set it active
                dispatchTurnOnMessage(cxt, request);
                break;
            case RECONNECTED:
                dispatchReconnectMessageType(cxt, request);
                break;
            case QR_CODE_EXCHANGE:
                dispatchQrCodeExchangeMessageType(cxt, request);
                break;
            case PRODUCT_LEVEL_ALERT:
                dispatchProductLowLevelMessageType(cxt, request);
                break;
            case MACHINE_FAIL:
                dispatchMachineFailMessageType(cxt, request);
                break;
            case PRODUCT_RECAP:
                dispatchMachineProductRecap(cxt, request);
                break;
        }
    }

    private void dispatchMachineProductRecap(ChannelHandlerContext cxt, MachineMessage request) {
        //get machine from connection
        Machine machine = repository.getMachine(request.getMachineId());
        //create a dto
        List<ProductRecap> recaps = new ArrayList();
        for (Product p : machine.getProducts()){
            recaps.add(new ProductRecap(p.getId(), repository.getSlotNumber(machine.getId(), p.getId())));
        }

        //create a json response
        String json = gson.toJson(recaps);
        cxt.channel().writeAndFlush(json + "\n");
    }

    private void dispatchMachineFailMessageType(ChannelHandlerContext cxt, MachineMessage request) {
        MachineFailMessage machineFailMessage = (MachineFailMessage)request.getMessage();
        //machine fail, need to create entity to register
        MachineFail fail = TransformationUtil.createMachineFail(machineFailMessage.getFailMessage(), machineFailMessage.getDate(),
                repository.getMachine(request.getMachineId()));

        //TODO: send error email
        repository.persistMachineFail(fail);
    }

    private void dispatchProductLowLevelMessageType(ChannelHandlerContext cxt, MachineMessage request) {
        ProductLevelMessage productLevelMessage = (ProductLevelMessage)request.getMessage();
        if (productLevelMessage.getProductLevelType().equals("alert")){
            //levels are below 30%
            //TODO: send email here to technician
        }else if(productLevelMessage.getProductLevelType().equals("warning")){
            //levels are below 15%
            //TODO: send email here to technician
        }
    }

    private void dispatchQrCodeExchangeMessageType(ChannelHandlerContext cxt, MachineMessage request) {
        final List<PreAuthPurchase> preAuthPurchases = exchangePurchases(request);
        final String json = gson.toJson(preAuthPurchases);
        cxt.channel().writeAndFlush(json + "\n");
    }

    private void dispatchReconnectMessageType(ChannelHandlerContext cxt, MachineMessage request) {
        final List<PreAuthPurchase> preAuthPurchases = exchangePurchases(request);
        final String json = gson.toJson(preAuthPurchases);
        cxt.channel().writeAndFlush(json + "\n");
    }

    private List<PreAuthPurchase> exchangePurchases(MachineMessage request){
        List<String> purchases = (ArrayList<String>)request.getMessage();
        List<PreAuthPurchase> preAuthPurchases = new ArrayList();
        Purchase oldPurchase;
        Purchase newPurchase;
        for (String purchaseId : purchases) {
            //change purchase status to cashed
            repository.updatePurchaseStatus(purchaseId, PurchaseStatus.CASHED);
            oldPurchase = repository.getPurchase(purchaseId);
            //create a new purchase
            newPurchase = createPurchase(request.getMachineId(), oldPurchase.getProductId());
            preAuthPurchases.add(TransformationUtil.createPreAuthPurchase(newPurchase, repository.getProductEngineUseTime(newPurchase.getProductId())));
        }
        return preAuthPurchases;
    }
}
