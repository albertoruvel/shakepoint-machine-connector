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
import com.shakepoint.web.io.service.QrCodeService;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;

@Startup
@Stateless
public class QrCodeServiceImpl implements QrCodeService {

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
    private String bucketName;

    private AmazonS3 amazonS3;

    private static final String S3_FORMAT = "https://%s.s3.amazonaws.com/%s";
    private static final Logger log = Logger.getLogger(QrCodeService.class);
    private final String qrCodeDataFormat = "%s_%s_%s_%s"; //purchase_machine_product_id

    @PostConstruct
    public void init(){
        amazonS3 = new AmazonS3Client(new AWSCredentialsProvider() {
            public AWSCredentials getCredentials() {
                return new BasicAWSCredentials(accessKey, secretKey);
            }

            public void refresh() {

            }
        });
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
            return uploadFile(file.getAbsolutePath());
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
        try{
            ObjectListing objectListing = amazonS3.listObjects(bucketName);
            while(true){
                for (Iterator<?> iterator =
                     objectListing.getObjectSummaries().iterator();
                     iterator.hasNext();) {
                    S3ObjectSummary summary = (S3ObjectSummary)iterator.next();
                    amazonS3.deleteObject(bucketName, summary.getKey());
                }

                // more object_listing to retrieve?
                if (objectListing.isTruncated()) {
                    objectListing= amazonS3.listNextBatchOfObjects(objectListing);
                } else {
                    break;
                }
            }
        }catch(Exception ex){
            log.error("Could not empty bucket", ex);
        }
    }

    private String uploadFile(String filePath){
        File  file = new File(filePath);
        String url = null;
        if(file.exists()){
            //choose bucket name and get fcm token
            try{
                //upload file
                final PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, file.getName(), file);
                putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);
                amazonS3.putObject(putObjectRequest);

                //generate url
                url = String.format(S3_FORMAT, bucketName, file.getName());
                //delete tmp file
                file.delete();
            } catch(ArrayIndexOutOfBoundsException ex){
                log.error(String.format("ArrayIndexOutOfBoundsException: %s", ex.getMessage()));
            } catch(AmazonServiceException ex){
                log.error(String.format("AmazonServiceException: %s", ex.getMessage()));
            } catch(SdkClientException ex){
                log.error(String.format("SdkClientException: %s", ex.getMessage()));
            } catch(Exception ex){
                log.error(ex.getMessage());
            }
        }else{
            log.error(String.format("Cannot upload file to S3 service: File %s does not exists on %s", file.getAbsolutePath(), tmpFolder));
        }
        return url;
    }
}
