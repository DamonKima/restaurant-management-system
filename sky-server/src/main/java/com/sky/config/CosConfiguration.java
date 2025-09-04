package com.sky.config;

import com.sky.properties.CosProperties;
import com.sky.utils.CosUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CosConfiguration {

    //初始化工具类中必备属性
    //aliyun使用一个工具类，腾讯云内置了一个，所以只需要一样传入信息就可以用了
    @Bean
    @ConditionalOnMissingBean
    public CosUtil cosUtil(CosProperties cosProperties) {
        log.info("开始创建腾讯云文件上传工具类对象：{}", cosProperties);
        return new CosUtil(cosProperties.getSecretId(),
                cosProperties.getSecretKey(),
                cosProperties.getRegion(),
                cosProperties.getBucketName());
    }
//
//    @Bean
//    @ConditionalOnMissingBean
//    public COSClient cosClient(CosProperties cosProperties) {
//        log.info("开始创建腾讯云文件上传工具类对象：{}", cosProperties);
//        //设置COS的访问域名区域
//        Region cosRegion = new Region(cosProperties.getRegion());
//        //设置HTTP请求的配置
//        ClientConfig clientConfig = new ClientConfig(cosRegion);
//        //初始化用户身份信息
//        COSCredentials cred = new BasicCOSCredentials(cosProperties.getSecretId(),
//                cosProperties.getSecretKey());
//        //创建COS客户端
//        return new COSClient(cred, clientConfig);
//    }

}