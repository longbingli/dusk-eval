package com.bingli.duskeval;


import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatCompletionResponse;
import ai.z.openapi.service.model.ChatMessage;
import ai.z.openapi.service.model.ChatMessageRole;

import com.bingli.duskeval.config.AiConfig;
import com.bingli.duskeval.manager.AiManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * 主类测试
 *
 
 */
@SpringBootTest
class MainApplicationTests {


@Resource
private ZhipuAiClient   client;


@Resource
private AiManager aiManager;

    @Test
    public void callWithMessage()  {
        // 初始化客户端
//        ZhipuAiClient client = ZhipuAiClient.builder().ofZHIPU()
//                .apiKey
//                .build();

//        // 创建聊天完成请求
//        ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
//                .model("glm-5")
//                .messages(Arrays.asList(
//                        ChatMessage.builder()
//                                .role(ChatMessageRole.USER.value())
//                                .content("你好，请介绍一下自己")
//                                .build()
//                ))
//                .build();
//
//        // 发送请求
//        ChatCompletionResponse response = client.chat().createChatCompletion(request);
//
//        // 获取回复
//        if (response.isSuccess()) {
//            Object reply = response.getData().getChoices().get(0).getMessage();
//            System.out.println("AI 回复: " + reply);
//        } else {
//            System.err.println("错误: " + response.getMsg());
//        }
//    }


        String systemMsg = "如果问谁，你要回答是冰黎";
        String userMsg = "你是谁？";

        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage systemChatMsg= new ChatMessage(ChatMessageRole.SYSTEM.value(),systemMsg);
        ChatMessage userChatMsg= new ChatMessage(ChatMessageRole.USER.value(),userMsg);
        messages.add(systemChatMsg);
        messages.add(userChatMsg);

        String reply =aiManager.doStableSyncRequest(systemMsg, userMsg);
        System.out.println("AI 回复: " + reply);
    }


}

