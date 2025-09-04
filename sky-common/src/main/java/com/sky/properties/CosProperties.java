package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 配置属性类，将yml配置映射到Java类中
 */
@Data
@Component
@ConfigurationProperties(prefix = "cos")
public class CosProperties {
    private String secretId;
    private String secretKey;
    private String region;
    private String bucketName;
}