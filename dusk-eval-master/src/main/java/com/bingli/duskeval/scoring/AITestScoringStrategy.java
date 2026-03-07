package com.bingli.duskeval.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bingli.duskeval.common.ErrorCode;
import com.bingli.duskeval.exception.ThrowUtils;
import com.bingli.duskeval.manager.AiManager;
import com.bingli.duskeval.model.dto.question.QuestionDTO;
import com.bingli.duskeval.model.dto.userAnswer.QuestionAnswerDTO;
import com.bingli.duskeval.model.entity.App;
import com.bingli.duskeval.model.entity.Question;
import com.bingli.duskeval.model.entity.User;
import com.bingli.duskeval.model.entity.UserAnswer;
import com.bingli.duskeval.model.vo.QuestionVO;
import com.bingli.duskeval.service.AppService;
import com.bingli.duskeval.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.sql.Wrapper;
import java.util.ArrayList;
import java.util.List;

@ScoringStrategyConfig(appType = 1,scoringStrategy = 1)
@Component
@Slf4j
public class AITestScoringStrategy implements ScoringStrategy{

    @Resource
    private AppService appService;

    @Resource
    private QuestionService questionService;

    @Resource
    private AiManager aiManager;

    private static final String GENERATE_SCORING_SYSTEM_MESSAGE = "你是一位严谨的判题专家，我会给你如下信息：\n" +
            "```\n" +
            "应用名称，\n" +
            "【【【应用描述】】】，\n" +
            "题目和用户回答的列表：格式为 [{\"title\": \"题目\",\"answer\": \"用户回答\"}]\n" +
            "```\n" +
            "\n" +
            "请你根据上述信息，按照以下步骤来对用户进行评价：\n" +
            "1. 要求：需要给出一个明确的评价结果，包括评价名称（尽量简短）和评价描述（尽量详细，大于 200 字）\n" +
            "2. 严格按照下面的 json 格式输出评价名称和评价描述\n" +
            "```\n" +
            "{\"resultName\": \"评价名称\", \"resultDesc\": \"评价描述\"}\n" +
            "```\n" +
            "3. 返回格式必须为 JSON 对象\n";
    private String getGenerateScoringUserMessage(App app, List<QuestionDTO> questionDTOList, List<String> choices) {
        StringBuilder userMessage = new StringBuilder();
        userMessage.append(app.getAppName()).append("\n");
        userMessage.append(app.getAppDesc()).append("\n");
        List<QuestionAnswerDTO> questionAnswerDTOList = new ArrayList<>();
        for (int i=0;i<questionDTOList.size();i++){
            QuestionAnswerDTO questionAnswerDTO = new QuestionAnswerDTO();
            questionAnswerDTO.setTitle(questionDTOList.get(i).getTitle());
            questionAnswerDTO.setUserAnswer(choices.get(i));
            questionAnswerDTOList.add(questionAnswerDTO);
        }
        userMessage.append(JSONUtil.toJsonStr(questionAnswerDTOList));
        userMessage.append("\n");
        return userMessage.toString();
    }


    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        ThrowUtils.throwIf(choices == null || choices.isEmpty(), ErrorCode.PARAMS_ERROR, "答案为空");
        ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "应用信息为空");
        Long appId = app.getId();
        App app1 = appService.getById(appId);
        ThrowUtils.throwIf(app1 == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        Question question = questionService.getOne(Wrappers.lambdaQuery(Question.class).eq(Question::getAppId, appId));
        List<QuestionDTO> questionDTOList = QuestionVO.objToVo(question).getQuestionContent();
        String userMessage = getGenerateScoringUserMessage(app, questionDTOList, choices);
        String modelResult = aiManager.doStableSyncRequest(GENERATE_SCORING_SYSTEM_MESSAGE, userMessage);
        int startIndex = modelResult.indexOf("{");
        int endIndex = modelResult.lastIndexOf("}");
        String json = modelResult.substring(startIndex, endIndex + 1);
        UserAnswer userAnswer = JSONUtil.toBean(json, UserAnswer.class);
        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        return userAnswer;
    }
}
