package com.shakepoint.web.io.service.impl;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.github.roar109.syring.annotation.ApplicationProperty;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.shakepoint.web.io.data.entity.Purchase;
import com.shakepoint.web.io.data.repository.MachineConnectionRepository;
import com.shakepoint.web.io.service.AWSS3Service;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Hashtable;
import java.util.Iterator;

@Startup
@Stateless
public class AWSS3ServiceImpl implements AWSS3Service {

    @Inject
    @ApplicationProperty(name = "com.shakepoint.web.io.qrcode.dir", type = ApplicationProperty.Types.SYSTEM)
    private String tmpFolder;

    @Inject
    @ApplicationProperty(name = "aws.accessKeyId", type = ApplicationProperty.Types.SYSTEM)
    private String accessKey;

    @Inject
    @ApplicationProperty(name = "aws.secretKey", type = ApplicationProperty.Types.SYSTEM)
    private String secretKey;

    @Inject
    @ApplicationProperty(name = "com.shakepoint.web.s3.qr.bucket.name", type = ApplicationProperty.Types.SYSTEM)
    private String qrCodeBucketName;

    @Inject
    @ApplicationProperty(name = "com.shakepoint.web.s3.nutritional.bucket.name", type = ApplicationProperty.Types.SYSTEM)
    private String nutritionalDataBucketName;

    @Inject
    @ApplicationProperty(name = "com.shakepoint.web.nutritional.tmp", type = ApplicationProperty.Types.SYSTEM)
    private String nutritionalDataTmpFolder;

    @Inject
    @ApplicationProperty(name = "com.shakepoint.web.rescue.qr.tmp", type = ApplicationProperty.Types.SYSTEM)
    private String qrCodesRescueFolder;

    @Inject
    private MachineConnectionRepository repository;

    private AmazonS3 amazonS3;

    private static final String S3_FORMAT = "https://%s.s3.amazonaws.com/%s";
    private static final Logger log = Logger.getLogger(AWSS3Service.class);
    private final String qrCodeDataFormat = "%s_%s_%s_%s"; //purchase_machine_product_id


    @PostConstruct
    public void init() {
        amazonS3 = new AmazonS3Client(new AWSCredentialsProvider() {
            public AWSCredentials getCredentials() {
                return new BasicAWSCredentials(accessKey, secretKey);
            }

            public void refresh() {

            }
        });
    }

    @Override
    public void createProductNutritionalData(String productId) {
        File file = new File(nutritionalDataTmpFolder + File.separator + productId + ".jpg");
        if (file.exists()) {
            String url = uploadFile(file, S3ImageType.NUTRITIONAL_DATA);
            repository.updateProductNutritionalDataUrl(productId, url);
        } else {
            log.error("Nutritional data image not found for id " + productId);
        }
    }

    @Override
    public String createQrCode(Purchase purchase) {
        String format = String.format(qrCodeDataFormat, purchase.getId(), purchase.getMachineId(),
                purchase.getProductId(), purchase.getPurchaseDate());

        String path = tmpFolder + File.separator + purchase.getId() + ".png";
        File file = new File(path);

        try {
            Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix byteMatrix = qrCodeWriter.encode(format, BarcodeFormat.QR_CODE, 256, 256, hintMap);
            int width = byteMatrix.getWidth();
            BufferedImage image = new BufferedImage(width, width,
                    BufferedImage.TYPE_INT_RGB);
            image.createGraphics();
            Graphics2D graphics = (Graphics2D) image.getGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, width);
            graphics.setColor(Color.BLACK);

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < width; j++) {
                    if (byteMatrix.get(i, j)) {
                        graphics.fillRect(i, j, 1, 1);
                    }
                }
            }
            ImageIO.write(image, "png", file);
            return uploadFile(file, S3ImageType.QR_CODE);
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void deleteAllQrCodes() {
        log.info("Will try to delete everything on bucket...");
        try {
            ObjectListing objectListing = amazonS3.listObjects(qrCodeBucketName);
            while (true) {
                for (Iterator<?> iterator =
                     objectListing.getObjectSummaries().iterator();
                     iterator.hasNext(); ) {
                    S3ObjectSummary summary = (S3ObjectSummary) iterator.next();
                    amazonS3.deleteObject(qrCodeBucketName, summary.getKey());
                }

                // more object_listing to retrieve?
                if (objectListing.isTruncated()) {
                    objectListing = amazonS3.listNextBatchOfObjects(objectListing);
                } else {
                    break;
                }
            }
        } catch (Exception ex) {
            log.error("Could not empty bucket", ex);
        }
    }

    @Override
    public void retryUpload(String purchaseId) {
        //get file from rescue folder
        File file = new File(qrCodesRescueFolder + File.separator + purchaseId + ".png");
        if (!file.exists()) {
            log.error("Cannot retry upload for nonexistent file, purchase " + purchaseId);
        } else {
            final String url = uploadFile(file, S3ImageType.QR_CODE);
            repository.updatePurchaseQrCodeUrl(purchaseId, url);
            log.info("File upload retry went successfully");
            //delete backup file
            file.delete();
        }
    }

    private String uploadFile(File file, S3ImageType imageType) {
        String url = null;
        if (file.exists()) {
            //choose bucket name and get fcm token
            try {
                String bucketName = null;
                switch (imageType) {
                    case NUTRITIONAL_DATA:
                        bucketName = nutritionalDataBucketName;
                        break;
                    case QR_CODE:
                        bucketName = qrCodeBucketName;
                        break;
                    default:
                        log.error("Invalid bucket name option");
                        return null;
                }
                //upload file
                final PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, file.getName(), file);
                putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);
                amazonS3.putObject(putObjectRequest);

                //generate url
                url = String.format(S3_FORMAT, bucketName, file.getName());
                //delete tmp file
                file.delete();
            } catch (Exception ex) {
                if (imageType == S3ImageType.QR_CODE) {
                    saveToRescueFolder(file);
                }
                log.error(ex.getMessage());
            }
        } else {
            log.error(String.format("Cannot upload file to S3 service: File %s does not exists on %s", file.getAbsolutePath(), tmpFolder));
        }
        return url;
    }

    private void saveToRescueFolder(final File file) {
        try {
            log.info("Saving qr code file to rescue folder");
            File outputFile = new File(qrCodesRescueFolder + File.separator + file.getName());
            InputStream in = new FileInputStream(file);
            OutputStream out = new FileOutputStream(outputFile);
            byte[] buffer = new byte[1028];

            while (in.read(buffer) != -1) {
                out.write(buffer);
            }
            in.close();
            out.close();

            //delete tmp file to store only on rescue folder
            file.delete();
        } catch (Exception ex) {
            log.error("Could not save qr code file to rescue folder", ex);
        }
    }

    public static enum S3ImageType {
        QR_CODE, NUTRITIONAL_DATA
    }
}
