package com.shakepoint.web.io.netty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shakepoint.web.io.data.dto.req.socket.MachineFailMessage;
import com.shakepoint.web.io.data.dto.req.socket.MachineMessage;
import com.shakepoint.web.io.data.dto.req.socket.MachineMessageType;
import com.shakepoint.web.io.data.dto.req.socket.ProductLevelMessage;
import com.shakepoint.web.io.data.dto.res.socket.PreAuthPurchase;
import com.shakepoint.web.io.data.dto.res.socket.ProductRecap;
import com.shakepoint.web.io.data.entity.*;
import com.shakepoint.web.io.data.repository.MachineConnectionRepository;
import com.shakepoint.web.io.email.Email;
import com.shakepoint.web.io.service.EmailService;
import com.shakepoint.web.io.service.QrCodeService;
import com.shakepoint.web.io.util.TransformationUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;


public class ChannelInboundHandler extends SimpleChannelInboundHandler<String> {

    private final String connectionId;
    private final int maxPrePurchases;
    private final MachineConnectionRepository repository;
    private final QrCodeService qrCodeService;
    private final EmailService emailService;
    private final Gson gson = new GsonBuilder().create();
    private Logger log = Logger.getLogger(getClass());

    public ChannelInboundHandler(String connectionId, MachineConnectionRepository repository,
                                 int maxPrePurchases, QrCodeService qrCodeService, EmailService emailService) {
        this.connectionId = connectionId;
        this.repository = repository;
        this.qrCodeService = qrCodeService;
        this.maxPrePurchases = maxPrePurchases;
        this.emailService = emailService;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MachineMessage request = null;
        try {
            String jsonMessage = (String) msg;
            request = gson.fromJson(jsonMessage, MachineMessage.class);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            dispatchNotValidMessageType(ctx, msg);
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

    private Purchase createPurchase(String machineId, String productId) {
        Purchase purchase = new Purchase();
        purchase.setMachineId(machineId);
        purchase.setProductId(productId);
        purchase.setPurchaseDate(TransformationUtil.DATE_FORMAT.format(new Date()));
        purchase.setStatus(PurchaseStatus.PRE_AUTH);
        purchase.setQrCodeUrl(qrCodeService.createQrCode(purchase));
        return purchase;
    }

    private void dispatchNotValidMessageType(ChannelHandlerContext cxt, Object msg) {
        String technicianEmail =  repository.getTechnicianEmailByMachineId(connectionId);
        if(technicianEmail == null){
            //If we don't have a default technician, send to admin
            technicianEmail = System.getProperty("com.shakepoint.web.admin.user");
        }
        final Machine m = repository.getMachine(connectionId);
        final Map<String,Object> params = new HashMap<String, Object>();
        params.put("message", String.valueOf(msg));
        params.put("machineId", connectionId);
        params.put("machineName", m.getName());
        emailService.sendEmail(Email.MACHINE_RECEIVED_NO_VALID_MESSAGE, technicianEmail, params);
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
        for (Product p : machine.getProducts()) {
            recaps.add(new ProductRecap(p.getId(), repository.getSlotNumber(machine.getId(), p.getId())));
        }

        //create a json response
        String json = gson.toJson(recaps);
        cxt.channel().writeAndFlush(json + "\n");
    }

    private void dispatchMachineFailMessageType(ChannelHandlerContext cxt, MachineMessage request) {
        Map<String, String> machineFailMessage = (Map) request.getMessage();
        //machine fail, need to create entity to register
        MachineFail fail = TransformationUtil.createMachineFail(machineFailMessage.get("failMessage"), machineFailMessage.get("date"),
                repository.getMachine(request.getMachineId()));

        Machine m = repository.getMachine(request.getMachineId());
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("machineId", request.getMachineId());
        params.put("machineName", m.getName());
        params.put("errorMessage", machineFailMessage.get("failMessage"));

        String technicianEmail =  repository.getTechnicianEmailByMachineId(request.getMachineId());
        if(technicianEmail == null){
            //If we don't have a default technician, send to admin
            technicianEmail = System.getProperty("com.shakepoint.web.admin.user");
        }
        emailService.sendEmail(Email.MACHINE_FAILED, technicianEmail, params);

        repository.persistMachineFail(fail);
    }

    private void dispatchProductLowLevelMessageType(ChannelHandlerContext cxt, MachineMessage request) {
        Map<String, String> productLevelMessage = (Map) request.getMessage();
        final Map<String, Object> params = new HashMap<String, Object>();

        final Machine machine =repository.getMachine(connectionId);
        final Product product = repository.getProductById(productLevelMessage.get("productId"));
        params.put("productName", product.getName());
        params.put("machineId", connectionId);
        params.put("machineName", machine.getName());

        String technicianEmail =  repository.getTechnicianEmailByMachineId(connectionId);
        if(technicianEmail == null){
            //If we don't have a default technician, send to admin
            technicianEmail = System.getProperty("com.shakepoint.web.admin.user");
        }

        if (productLevelMessage.containsKey("productLevelType") &&
                productLevelMessage.get("productLevelType").equals("alert")) {
            //levels are below 30%
            emailService.sendEmail(Email.PRODUCT_LOW_LEVEL_ALERT, technicianEmail, params);
        } else if (productLevelMessage.containsKey("productLevelType") &&
                productLevelMessage.get("productLevelType").equals("warning")) {
            //levels are below 15%
            emailService.sendEmail(Email.PRODUCT_LOW_LEVEL_CRITICAL, technicianEmail, params);
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

    private List<PreAuthPurchase> exchangePurchases(MachineMessage request) {
        List<String> purchases = (ArrayList<String>) request.getMessage();
        List<PreAuthPurchase> preAuthPurchases = new ArrayList();
        Purchase oldPurchase;
        Purchase newPurchase;
        for (String purchaseId : purchases) {
            //change purchase status to cashed
            repository.updatePurchaseStatus(purchaseId, PurchaseStatus.CASHED);
            oldPurchase = repository.getPurchase(purchaseId);
            if (oldPurchase == null){
                log.error(String.format("No purchase found with ID %s", purchaseId));
            }else {
                //create a new purchase
                newPurchase = createPurchase(request.getMachineId(), oldPurchase.getProductId());
                preAuthPurchases.add(TransformationUtil.createPreAuthPurchase(newPurchase, repository.getProductEngineUseTime(newPurchase.getProductId())));
            }

        }
        return preAuthPurchases;
    }
}
