package com.bingli.duskeval.scoring;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bingli.duskeval.aop.LogInterceptor;
import com.bingli.duskeval.model.entity.App;
import com.bingli.duskeval.model.entity.Question;
import com.bingli.duskeval.model.entity.ScoringResult;
import com.bingli.duskeval.model.entity.UserAnswer;
import com.bingli.duskeval.scoring.ScoringStrategy;
import com.bingli.duskeval.scoring.ScoringStrategyConfig;
import com.bingli.duskeval.service.AppService;
import com.bingli.duskeval.service.QuestionService;
import com.bingli.duskeval.service.ScoringResultService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import springfox.documentation.spring.web.json.Json;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 自定义得分类评分策略
 * 适用于得分类应用（appType = 0）的自定义评分策略（scoringStrategy = 0）
 */

@ScoringStrategyConfig(appType = 0, scoringStrategy = 0)
@Component // 确保类被 Spring 管理
@Slf4j // 增加日志记录
public class CustomScoreScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;


    @Resource
    private ScoringResultService scoringResultService;

    /**
     * 执行评分逻辑
     *
     * @param choices 用户选择的答案列表
     * @param app     应用信息
     * @return 评分结果
     * @throws IllegalArgumentException 若输入参数非法
     * @throws IllegalStateException    若查询不到题目或评分结果
     */
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        // 1. 参数校验
        if (choices == null || choices.isEmpty()) {
            throw new IllegalArgumentException("用户答案不能为空");
        }
        if (app == null || app.getId() == null) {
            throw new IllegalArgumentException("应用信息不能为空");
        }

        Long appId = app.getId();

        // 2. 根据应用ID查询题目和评分结果
        Question question = questionService.getOne(
                Wrappers.lambdaQuery(Question.class)
                        .eq(Question::getAppId, appId)
        );
        if (question == null) {
            throw new IllegalStateException("未找到对应的应用题目，appId: " + appId);
        }
        String questionContentJson = question.getQuestionContent();

        if (questionContentJson == null || questionContentJson.isEmpty()) {
            throw new IllegalStateException("题目内容不能为空");
        }

        List<ScoringResult> scoringResults = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class)
                        .eq(ScoringResult::getAppId, appId)
        );
        if (scoringResults == null || scoringResults.isEmpty()) {
            throw new IllegalStateException("未找到对应的应用评分结果配置，appId: " + appId);
        }

        // 3. 计算分数（优化逻辑：使用Map提高查找效率）
        int totalScore = calculateScore(choices, questionContentJson);

        // 4. 根据得分查找对应的评分结果
        String resultName = "未找到结果";
        String resultDesc = "无法判断";
        for (ScoringResult scoringResult : scoringResults) {
            Integer scoreRange = scoringResult.getResultScoreRange();
            if (scoreRange != null && totalScore >= scoreRange) {
                resultName = scoringResult.getResultName();
                resultDesc = scoringResult.getResultDesc() != null ? scoringResult.getResultDesc() : resultDesc;
                break;
            }
        }



        // 6. 构造并返回用户答案对象
        UserAnswer userAnswer = buildUserAnswer(appId, app, choices, resultName, resultDesc, totalScore);
        return userAnswer;
    }

    /**
     * 计算总分
     */
    public static int calculateScore(List<String> choices, String questionContentJson) throws Exception {
        int totalScore = 0;

        // 解析问题内容的 JSON 字符串
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> questions;
        try {
            questions = objectMapper.readValue(questionContentJson, List.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("解析问题内容失败", e);
        }

        // 校验题目数量与答案数量一致
        if (choices.size() != questions.size()) {
            throw new IllegalArgumentException("用户答案与题目数量不匹配");
        }

        // 通过 HashMap 优化查找选项的效率
        for (int i = 0; i < questions.size(); i++) {
            // 获取当前题目的选项
            Map<String, Object> question = questions.get(i);
            List<Map<String, Object>> options = (List<Map<String, Object>>) question.get("options");

            // 创建一个 HashMap 用于快速查找选项
            Map<String, Integer> optionMap = new HashMap<>();
            for (Map<String, Object> option : options) {
                String key = (String) option.get("key");
                int score = (int) option.get("score");
                optionMap.put(key, score);
            }

            // 获取用户选择的答案
            String userChoice = choices.get(i);
            if (optionMap.containsKey(userChoice)) {
                totalScore += optionMap.get(userChoice);
            }
        }
        return totalScore; // 返回总分
    }

    /**
     * 构造用户答案对象
     */
    private UserAnswer buildUserAnswer(Long appId, App app, List<String> choices, String resultName, String resultDesc, int totalScore) {
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(String.join(",", choices)); // 使用 String.join 更加规范
        userAnswer.setResultName(resultName);
        userAnswer.setResultDesc(resultDesc);
        userAnswer.setResultScore(totalScore);
        return userAnswer;
    }
}