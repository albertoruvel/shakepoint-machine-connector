package com.shakepoint.web.io.util;

import com.shakepoint.web.io.data.dto.res.socket.PreAuthPurchase;
import com.shakepoint.web.io.data.entity.Purchase;
import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

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
}
