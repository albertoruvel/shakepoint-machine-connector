package com.shakepoint.web.io.util;

import com.shakepoint.web.io.data.dto.res.socket.PreAuthPurchase;
import com.shakepoint.web.io.data.entity.Machine;
import com.shakepoint.web.io.data.entity.MachineConnection;
import com.shakepoint.web.io.data.entity.MachineFail;
import com.shakepoint.web.io.data.entity.Purchase;
import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TransformationUtil {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("mm/dd/yyyy hh:mm:ss a");

    private static Logger log = Logger.getLogger(TransformationUtil.class);

    public static Collection<PreAuthPurchase> createPreAuthPurchases(List<Purchase> purchases) {

        List<PreAuthPurchase> prePurchases = new ArrayList();
        for (Purchase purchase : purchases) {
            prePurchases.add(createPreAuthPurchase(purchase));
        }
        return prePurchases;
    }

    public static PreAuthPurchase createPreAuthPurchase(Purchase purchase) {
        return new PreAuthPurchase(purchase.getId(), purchase.getProductId(), purchase.getPurchaseDate());
    }

    public static MachineConnection createMachineConnection(String machineId, int fromPort, int toPort){
        int randomPort = ThreadLocalRandom.current().nextInt(fromPort, toPort + 1);
        MachineConnection connection = new MachineConnection();
        connection.setConnectionActive(false);
        connection.setLastUpdate(TransformationUtil.DATE_FORMAT.format(new Date()));
        connection.setMachineId(machineId);
        connection.setMachineToken(UUID.randomUUID().toString());
        connection.setPort(randomPort);
        return connection;
    }

    public static MachineFail createMachineFail(String failMessage, String date, Machine machine) {
        MachineFail fail = new MachineFail();
        fail.setDate(date);
        fail.setMessage(failMessage);
        fail.setMachine(machine);
        return fail;
    }
}
