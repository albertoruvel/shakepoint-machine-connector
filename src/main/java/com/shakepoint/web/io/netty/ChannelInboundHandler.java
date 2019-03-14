package com.shakepoint.web.io.netty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shakepoint.web.io.data.dto.req.socket.MachineMessage;
import com.shakepoint.web.io.data.dto.req.socket.MachineMessageType;
import com.shakepoint.web.io.data.dto.res.socket.PreAuthPurchase;
import com.shakepoint.web.io.data.dto.res.socket.ProductRecap;
import com.shakepoint.web.io.data.dto.res.socket.ReplacementCheck;
import com.shakepoint.web.io.data.entity.*;
import com.shakepoint.web.io.data.repository.MachineConnectionRepository;
import com.shakepoint.web.io.email.Email;
import com.shakepoint.web.io.service.EmailService;
import com.shakepoint.web.io.service.AWSS3Service;
import com.shakepoint.web.io.util.TransformationUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


public class ChannelInboundHandler extends SimpleChannelInboundHandler<String> {

    private final String connectionId;
    private final int maxPrePurchases;
    private final MachineConnectionRepository repository;
    private final AWSS3Service AWSS3Service;
    private final EmailService emailService;
    private final Gson gson = new GsonBuilder().create();
    private Logger log = Logger.getLogger(getClass());

    public ChannelInboundHandler(String connectionId, MachineConnectionRepository repository,
                                 int maxPrePurchases, AWSS3Service AWSS3Service, EmailService emailService) {
        this.connectionId = connectionId;
        this.repository = repository;
        this.AWSS3Service = AWSS3Service;
        this.maxPrePurchases = maxPrePurchases;
        this.emailService = emailService;
    }

    private void dispatchPurchasesReplacementsForMachine(ChannelHandlerContext cxt, MachineMessage request) {
        //get purchases where there are no more products registered
        List<MachineProductStatus> currentVendingProducts = repository.getMachineProducts(request.getMachineId());
        List<Purchase> preAuthPurchases = repository.getPreAuthorizedPurchasesForMachine(request.getMachineId());
        //get products from purchases
        Map<String, Object> actualProducts = new HashMap<String, Object>();
        preAuthPurchases.stream().forEach(purchase -> actualProducts.put(purchase.getProductId(), purchase));
        Map<String, Object> currentProductIds = new HashMap<String, Object>();
        currentVendingProducts.stream().forEach(status -> currentProductIds.put(status.getProductId(), status));
        //compare id's
        List<String> preAuthProductsIds = actualProducts.keySet().stream().collect(Collectors.toList());
        List<String> currentProductsIds = currentProductIds.keySet().stream().collect(Collectors.toList());
        //sort them
        Collections.sort(preAuthProductsIds, Comparator.comparing(String::toString));
        Collections.sort(currentProductsIds, Comparator.comparing(String::toString));
        log.info(String.format("Current products id's from purchases [%s]", preAuthProductsIds));
        log.info(String.format("Current products id's from vending [%s]", currentProductsIds));

        Boolean machineChanges = ! preAuthProductsIds.equals(currentProductsIds);
        ReplacementCheck check = new ReplacementCheck(machineChanges);
        log.info(String.format("Machine have changed: %s", machineChanges ? "No" : "Yes"));
        final String json = gson.toJson(check);
        log.info(json);
        cxt.channel().writeAndFlush(json + "\n");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MachineMessage request = null;
        try {
            String jsonMessage = (String) msg;
            request = gson.fromJson(jsonMessage, MachineMessage.class);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            log.info(msg);
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
        repository.updateMachineConnectionStatus(connectionId, false);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
        log.info(s);
    }


    private void dispatchTurnOnMessage(ChannelHandlerContext cxt, MachineMessage request) {
        final String machineId = request.getMachineId();
        log.info(String.format("Got turn on message from machine %s", request.getMachineId()));
        //check for existing purchases with status pre_auth
        List<Purchase> machinePurchases = repository.getMachinePreAuthorizedPurchases(machineId);
        List<PreAuthPurchase> preAuthPurchases = new ArrayList();
        machinePurchases.stream().forEach(purchase -> repository.updatePurchaseStatus(purchase.getId(), PurchaseStatus.CANCELLED));
        //create new purchases
        List<MachineProductStatus> vendingProducts = repository.getMachineProducts(request.getMachineId());
        log.info(String.format("Machine has %d registered products", vendingProducts.size()));
        log.info("Creating purchases");
        for (MachineProductStatus status : vendingProducts) {
            try {
                for (int i = 0; i < maxPrePurchases; i++) {
                    Purchase purchase = createPurchase(request.getMachineId(), status.getProductId());
                    log.info(String.format("Created purchase with ID: " + purchase.getId()));
                    repository.addPurchase(purchase);
                    Product product = repository.getProductById(status.getProductId());
                    Integer slotNumber = repository.getSlotNumber(request.getMachineId(), status.getProductId());
                    PreAuthPurchase preAuthPurchase = TransformationUtil.createPreAuthPurchase(purchase,
                            String.valueOf(product.getEngineUseTime()), slotNumber);
                    preAuthPurchases.add(preAuthPurchase);
                }
            } catch (Exception ex) {
                log.error(ex);
            }
        }
        //remove cancelled purchases
        log.info("Deleting cancelled purchases");
        repository.removeCancelledPurchases(machineId);
        preAuthPurchases.sort(Comparator.comparingInt(PreAuthPurchase::getSlot));
        //create a json response
        final String json = gson.toJson(preAuthPurchases);
        cxt.channel().writeAndFlush(json + "\n");
    }

    private Purchase createPurchase(String machineId, String productId) {
        Purchase purchase = new Purchase();
        purchase.setMachineId(machineId);
        purchase.setProductId(productId);
        purchase.setPurchaseDate(TransformationUtil.DATE_FORMAT.format(new Date()));
        purchase.setStatus(PurchaseStatus.PRE_AUTH);
        purchase.setQrCodeUrl(AWSS3Service.createQrCode(purchase));
        purchase.setControlNumber(createControlNumber());
        Product p = repository.getProductById(productId);
        purchase.setTotal(p.getPrice());
        return purchase;
    }

    private void dispatchNotValidMessageType(ChannelHandlerContext cxt, Object msg) {
        String technicianEmail = repository.getTechnicianEmailByMachineId(connectionId);
        if (technicianEmail == null) {
            //If we don't have a default technician, send to admin
            technicianEmail = System.getProperty("com.shakepoint.web.admin.user");
        }
        final Machine m = repository.getMachine(connectionId);
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("message", String.valueOf(msg));
        params.put("machineId", connectionId);
        params.put("machineName", m.getName());
        emailService.sendEmail(Email.MACHINE_RECEIVED_NO_VALID_MESSAGE, technicianEmail, params);
    }

    private String createControlNumber() {
        final String controlNumber = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return controlNumber;
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
            case PRODUCT_REPLACEMENT:
                dispatchPurchasesReplacementsForMachine(cxt, request);
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

        String technicianEmail = repository.getTechnicianEmailByMachineId(request.getMachineId());
        if (technicianEmail == null) {
            //If we don't have a default technician, send to admin
            technicianEmail = System.getProperty("com.shakepoint.web.admin.user");
        }
        emailService.sendEmail(Email.MACHINE_FAILED, technicianEmail, params);

        repository.persistMachineFail(fail);
    }

    private void dispatchProductLowLevelMessageType(ChannelHandlerContext cxt, MachineMessage request) {
        log.info("Received low level message from machine " + request.getMachineId());
        Map<String, String> productLevelMessage = (Map) request.getMessage();
        final Map<String, Object> params = new HashMap<String, Object>();

        final Machine machine = repository.getMachine(connectionId);
        final Product product = repository.getProductById(productLevelMessage.get("productId"));
        params.put("productName", product.getName());
        params.put("machineId", request.getMachineId());
        params.put("machineName", machine.getName());


        final List<String> emails = repository.getAdminsAndTechniciansEmails(machine.getTechnicianId());

        //send emails...
        for (String email : emails) {
            emailService.sendEmail(Email.PRODUCT_LOW_LEVEL_ALERT, email, params);
        }

        log.info("Product low level emails sent");
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
            //check if
            //change purchase status to cashed
            repository.updatePurchaseStatus(purchaseId, PurchaseStatus.CASHED);
            oldPurchase = repository.getPurchase(purchaseId);
            if (oldPurchase == null) {
                log.error(String.format("No purchase found with ID %s", purchaseId));
            } else {
                //get machine id
                MachineConnection connection = repository.getConnection(connectionId);
                //get slot number
                Integer slot = repository.getSlotNumber(connection.getMachineId(), oldPurchase.getProductId());
                //create a new purchase
                newPurchase = createPurchase(request.getMachineId(), oldPurchase.getProductId());
                repository.addPurchase(newPurchase);
                PreAuthPurchase purchase = TransformationUtil.createPreAuthPurchase(newPurchase, repository.getProductEngineUseTime(newPurchase.getProductId()), slot);
                preAuthPurchases.add(purchase);
                log.info(String.format("Exchanged purchase with ID %s at %s", newPurchase.getId(), TransformationUtil.DATE_FORMAT.format(new Date())));
            }

        }
        log.info(String.format("Exchanged %d purchases for machine %s", preAuthPurchases.size(), request.getMachineId()));
        preAuthPurchases.sort(Comparator.comparing(PreAuthPurchase::getSlot));
        log.info("Sorted purchases list by slot number");
        return preAuthPurchases;
    }
}
