package com.shakepoint.web.io.netty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shakepoint.web.io.data.dto.req.socket.MachineMessage;
import com.shakepoint.web.io.data.dto.req.socket.MachineMessageType;
import com.shakepoint.web.io.data.dto.res.socket.PreAuthPurchase;
import com.shakepoint.web.io.data.dto.res.socket.ProductRecap;
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
import java.util.*;


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

        //check for existing purchases with status pre_auth
        List<Purchase> machinePurchases = repository.getMachinePreAuthorizedPurchases(machineId);
        List<Product> availableProducts = repository.getMachineAvailableProducts(request.getMachineId());
        List<PreAuthPurchase> preAuthPurchases = new ArrayList();
        if (machinePurchases.isEmpty()) {
            log.info("Machine purchases are empty, will create new ones");
            preAuthPurchases.addAll(TransformationUtil.createPreAuthPurchases(createPurchases(machineId), repository));
        } else {
            log.info(String.format("Available purchases for machine: %d", availableProducts.size()));
            for (Product p : availableProducts) {
                //get needed purchases for this product on machine
                int neededPurchases = repository.getNeededPurchasesByProductOnMachine(p.getId(), machineId, maxPrePurchases);
                log.info(String.format("will create %d purchases", neededPurchases));
                for (int j = 0; j < neededPurchases; j++) {
                    Purchase purchase = createPurchase(machineId, p.getId());
                    repository.addPurchase(purchase);
                    Integer slot = repository.getSlotNumber(machineId, purchase.getProductId());
                    preAuthPurchases.add(TransformationUtil.createPreAuthPurchase(purchase, String.valueOf(p.getEngineUseTime()), slot));
                }
            }
            //create (maxPrePurchases - size) purchases
            preAuthPurchases.addAll(TransformationUtil.createPreAuthPurchases(
                    createPurchases(machineId), repository));
            //preAuthPurchases.sort(Comparator.comparing(PreAuthPurchase::getSlot));
        }
        //order by slot
        log.info("Sorted purchases by slot number");
        //create a json response
        final String json = gson.toJson(preAuthPurchases);

        log.info("Sending pre authorized codes to client " + connectionId);
        cxt.channel().writeAndFlush(json + "\n");
    }

    private List<Purchase> createPurchases(String machineId) {
        //get products for machine
        List<Product> products = repository.getMachineAvailableProducts(machineId);
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
            case PRODUCTS_REPLACEMENT:
                dispatchMachineProductsReplacement(cxt, request);
                break;
        }
    }

    private void dispatchMachineProductsReplacement(ChannelHandlerContext cxt, MachineMessage request) {
        //get all products for machine
        List<VendingProductStatus> vendingProducts = repository.getVendingProducts(request.getMachineId());

        //create a new list for new purchases replacements
        List<PreAuthPurchase> newPreAuthPurchases = new ArrayList();
        /**vendingProducts.stream().forEach(vendingProductStatus -> {
            //check if there are any pre auth purchase on machine with this product id and machine id
            final List<Purchase> preAuthePurchasesForProduct = repository.getMachinePreAuthorizedPurchasesForProduct(vendingProductStatus.getMachineId(), vendingProductStatus.getProductId());
            //set all purchases as cancelled
            preAuthePurchasesForProduct.stream().forEach(preAuthPurchase -> {
                repository.updatePurchaseStatus(preAuthPurchase.getId(), PurchaseStatus.CANCELLED);
            });
            //create new purchases

        });**/
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
                preAuthPurchases.add(TransformationUtil.createPreAuthPurchase(newPurchase, repository.getProductEngineUseTime(newPurchase.getProductId()), slot));
            }

        }
        log.info(String.format("Exchanged %d purchases for machine %s", preAuthPurchases.size(), request.getMachineId()));
        preAuthPurchases.sort(Comparator.comparing(PreAuthPurchase::getSlot));
        log.info("Sorted purchases list by slot number");
        return preAuthPurchases;
    }
}
