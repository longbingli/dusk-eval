package com.bingli.duskeval.config;





import ai.z.openapi.ZhipuAiClient;
import com.bingli.duskeval.manager.AiManager;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@Data
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    /**
     * AI API Key
     */
    private String apiKey;
    /**
     * 模型名称
     */
    private String model;

    @Bean
    public ZhipuAiClient zhipuAiClient(){
        return   ZhipuAiClient.builder().ofZHIPU()
                .apiKey(apiKey)
                .build();

    }


}
