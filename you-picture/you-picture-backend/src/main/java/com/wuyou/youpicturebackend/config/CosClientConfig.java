package com.wuyou.youpicturebackend.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 将 Client 的客户端配置注入成 bean 对象，以便后面使用上传下载的时候引入客户端，和配置的读取
 */
@Configuration
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosClientConfig {

    //域名
    private String host;
    private String secretId;
    //密钥
    private String secretKey;
    //区域
    private String region;
    //桶名
    private String bucket;

    @Bean
    public COSClient cosClient(){
        // 1 初始化用户身份信息（secretId, secretKey）。
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2 设置 bucket 的地域
        // clientConfig 中包含了设置 region, https(默认 http), 超时, 代理等 set 方法
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        // 3 生成 cos 客户端。
       return new COSClient(cred, clientConfig);
    }
}