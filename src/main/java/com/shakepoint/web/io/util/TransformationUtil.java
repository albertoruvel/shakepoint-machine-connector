package com.shakepoint.web.io.util;

import com.shakepoint.web.io.data.dto.res.socket.PreAuthPurchase;
import com.shakepoint.web.io.data.dto.res.socket.RefreshedPurchase;
import com.shakepoint.web.io.data.entity.Machine;
import com.shakepoint.web.io.data.entity.MachineConnection;
import com.shakepoint.web.io.data.entity.MachineFail;
import com.shakepoint.web.io.data.entity.Purchase;
import com.shakepoint.web.io.data.repository.MachineConnectionRepository;
import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TransformationUtil {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss a");

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT-7"));
    }

    private static Logger log = Logger.getLogger(TransformationUtil.class);

    public static PreAuthPurchase createPreAuthPurchase(Purchase purchase, String engineUseTime, Integer slotNumber, Integer mixTime) {
        try {
            Long useTime = Long.parseLong(engineUseTime);
            return new PreAuthPurchase(purchase.getId(), purchase.getProductId(), purchase.getPurchaseDate(), useTime, slotNumber, mixTime);
        } catch (Exception ex) {
            log.error(ex);
            return new PreAuthPurchase(purchase.getId(), purchase.getProductId(), purchase.getPurchaseDate(), 0L, slotNumber, mixTime);
        }

    }

    public static MachineConnection createMachineConnection(String machineId, int fromPort, int toPort) {
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
