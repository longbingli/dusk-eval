package com.bingli.duskeval.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bingli.duskeval.common.ErrorCode;
import com.bingli.duskeval.exception.ThrowUtils;
import com.bingli.duskeval.model.dto.question.QuestionDTO;
import com.bingli.duskeval.model.entity.App;
import com.bingli.duskeval.model.entity.Question;
import com.bingli.duskeval.model.entity.ScoringResult;
import com.bingli.duskeval.model.entity.UserAnswer;
import com.bingli.duskeval.model.vo.QuestionVO;
import com.bingli.duskeval.service.AppService;
import com.bingli.duskeval.service.QuestionService;
import com.bingli.duskeval.service.ScoringResultService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试类题的评分策略
 */
@Slf4j
@Component
@ScoringStrategyConfig(appType = 1,scoringStrategy = 0)
public class CustomTestScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;
    @Resource
    private AppService appService;

    @Resource
    private ScoringResultService scoringResultService;

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
    ThrowUtils.throwIf(StringUtils.isBlank(question.getQuestionContent()), ErrorCode.PARAMS_ERROR, "题目内容为空或格式错误");

    List<ScoringResult> scoringResults = scoringResultService.list(
            Wrappers.lambdaQuery(ScoringResult.class)
                    .eq(ScoringResult::getAppId, appId)
    );
    if (scoringResults.isEmpty()) {
        throw new IllegalStateException("未找到对应的应用评分结果配置，appId: " + appId);
    }

    // 3. 解析题目内容并统计用户选择的属性个数
    Map<String, Integer> optionCount = new HashMap<>();
    QuestionVO questionVO = QuestionVO.objToVo(question);
    List<QuestionDTO> questionContent = questionVO.getQuestionContent();

    if (questionContent == null || questionContent.isEmpty()) {
        throw new IllegalStateException("题目内容为空或格式错误");
    }

    for (int i = 0; i < Math.min(choices.size(), questionContent.size()); i++) {
        String userChoice = choices.get(i);
        QuestionDTO questionDTO = questionContent.get(i);

        questionDTO.getOptions().stream()
                .filter(option -> option.getKey().equals(userChoice))
                .findFirst()
                .ifPresent(option -> optionCount.merge(option.getResult(), 1, Integer::sum));
    }

    // 4. 计算最高得分结果
    ScoringResult bestResult = scoringResults.stream()
            .max(Comparator.comparingInt(result -> {
                try {

                    List<String> resultProps = JSONUtil.toList(result.getResultProp(), String.class);
                    return resultProps.stream()
                            .mapToInt(prop -> optionCount.getOrDefault(prop, 0))
                            .sum();
                } catch (Exception e) {
                    log.warn("解析评分结果属性失败，resultId: {}, resultProp: {}", result.getId(), result.getResultProp(), e);
                    return 0; // 忽略无效评分结果
                }
            }))
            .orElseThrow(() -> new IllegalStateException("未找到匹配的评分结果"));

    // 5. 构造返回值，填充答案对象的属性
    UserAnswer userAnswer = new UserAnswer();
    userAnswer.setAppId(appId);
    userAnswer.setAppType(app.getAppType());
    userAnswer.setScoringStrategy(app.getScoringStrategy());
    userAnswer.setChoices(JSONUtil.toJsonStr(choices));
    userAnswer.setResultId(bestResult.getId());
    userAnswer.setResultName(bestResult.getResultName());
    userAnswer.setResultDesc(bestResult.getResultDesc());
    userAnswer.setResultPicture(bestResult.getResultPicture());
    userAnswer.setResultScore(
            JSONUtil.toList(bestResult.getResultProp(), String.class).stream()
                    .mapToInt(prop -> optionCount.getOrDefault(prop, 0))
                    .sum()
    );

    return userAnswer;
}

}
