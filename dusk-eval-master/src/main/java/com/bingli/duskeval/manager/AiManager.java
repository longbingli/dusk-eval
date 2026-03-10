package com.bingli.duskeval.manager;


import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.*;
import com.bingli.duskeval.common.ErrorCode;
import com.bingli.duskeval.config.AiConfig;
import com.bingli.duskeval.exception.ThrowUtils;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class AiManager {

    private static final float STABLE_TEMPERATURE  = 0.05f;
    private static final float UNSTABLE_TEMPERATURE  = 0.90f;

    @Resource
    private ZhipuAiClient zhipuAiClient;

    @Resource
    private AiConfig aiConfig;

    public Flowable<ModelData> doStreamStableRequest(String systemMsg, String userMsg) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMsg));
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), userMsg));
        return doStreamRequest(messages, STABLE_TEMPERATURE);
    }
    public Flowable<ModelData> doStreamRequest(List<ChatMessage> messages,Float temperature){
        ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                .model(aiConfig.getModel())
                .stream(true)
                .temperature(temperature)
                .messages(messages)
                .build();


        ChatCompletionResponse response = zhipuAiClient.chat().createChatCompletion(request);
        return response.getFlowable();
    }

/**
     * AI同步请求(答案较稳定)
     * @param systemMsg
     * @param userMsg
     * @return
     */
    public String doStableSyncRequest(String systemMsg, String userMsg){

        return doRequest(systemMsg,userMsg,false,STABLE_TEMPERATURE);

    }
    /**
     * AI同步请求（答案较随即你）
     * @param systemMsg
     * @param userMsg
     * @return
     */
    public String doUnStableSyncRequest(String systemMsg, String userMsg){

        return doRequest(systemMsg,userMsg,false,UNSTABLE_TEMPERATURE);

    }

/**
     * AI同步请求
     * @param systemMsg
     * @param userMsg
     * @param temperature
     * @return
     */
public String doSyncRequest(String systemMsg, String userMsg,Float  temperature){

    return doRequest(systemMsg,userMsg,false,temperature);

}

/**
     * 通用AI请求
     * @param systemMsg
     * @param userMsg
     * @param stream
     * @param temperature
     * @return
     */
    public String doRequest(String systemMsg, String userMsg,Boolean stream,Float temperature){

        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage systemChatMsg= new ChatMessage(ChatMessageRole.SYSTEM.value(),systemMsg);
        ChatMessage userChatMsg= new ChatMessage(ChatMessageRole.USER.value(),userMsg);

        messages.add(systemChatMsg);
        messages.add(userChatMsg);
        // 获取回复
        return doRequest(messages,stream,temperature);

    }

    /**
     * 通用AI请求
     * @param messages
     * @param stream
     * @param temperature
     * @return
     */
    public String doRequest(List<ChatMessage> messages,Boolean stream,Float temperature){

            ThrowUtils.throwIf(messages == null || messages.isEmpty(), ErrorCode.PARAMS_ERROR, "消息列表不能为空");
            ThrowUtils.throwIf(aiConfig == null || aiConfig.getModel() == null, ErrorCode.SYSTEM_ERROR, "AI配置未初始化");

        ChatCompletionCreateParams request = ChatCompletionCreateParams.builder()
                .model(aiConfig.getModel())
                .stream(stream)
                .temperature(temperature)
                .messages(messages)
                .build();
        log.info("请求信息：{}", request);
        ChatCompletionResponse response = zhipuAiClient.chat().createChatCompletion(request);

        // 获取回复
        try {
                ThrowUtils.throwIf(response == null, ErrorCode.SYSTEM_ERROR, "请求失败：响应为空");
                ThrowUtils.throwIf(response.getData() == null, ErrorCode.SYSTEM_ERROR, "请求失败：响应数据为空");
                ThrowUtils.throwIf(response.getData().getChoices() == null || response.getData().getChoices().isEmpty(), ErrorCode.SYSTEM_ERROR, "请求失败：响应数据格式错误");
                ThrowUtils.throwIf(response.getData().getChoices().get(0).getMessage() == null, ErrorCode.SYSTEM_ERROR, "请求失败：回复内容为空");

            ChatMessage replyMessage = response.getData().getChoices().get(0).getMessage();
            String reply = replyMessage.getContent().toString();

            log.info("AI回复：{}", reply);
            
            return reply;
        } catch (Exception e) {
            log.error("处理AI响应时发生异常，request: {}", request);
            log.error("处理AI响应时发生异常，response: {}", response);
            log.error("处理AI响应时发生异常", e);
            return null;
        }
    }







}
