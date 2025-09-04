package com.sky.utils;


import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.region.Region;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Data
@AllArgsConstructor
@Slf4j
public class CosUtil {

    private String secretId;
    private String secretKey;
    private String region;
    private String bucketName;

    /**
     * 自定义腾讯云文件上传工具类
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {
        COSClient cosClient = new COSClient(new BasicCOSCredentials(secretId, secretKey), new ClientConfig(new Region(region)));
        String key = "upload/" + objectName;
        InputStream inputStream = new ByteArrayInputStream(bytes);
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, inputStream, null);
        cosClient.putObject(putObjectRequest);
        return "https://" + bucketName + ".cos." + region + ".myqcloud.com/" + key;
    }
}