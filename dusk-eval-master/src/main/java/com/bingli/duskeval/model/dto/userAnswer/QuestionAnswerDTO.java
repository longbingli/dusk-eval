package com.bingli.duskeval.model.dto.userAnswer;

import lombok.Data;

@Data
public class QuestionAnswerDTO {

    /**
     * 题目
     */
    private String title;

    /**
     * 用户答案
     */
    private String userAnswer;
}
