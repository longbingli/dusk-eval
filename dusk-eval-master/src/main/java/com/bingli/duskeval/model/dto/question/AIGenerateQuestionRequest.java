package com.bingli.duskeval.model.dto.question;


import lombok.Data;

import java.io.Serializable;
/**
 * AI生成问题请求
 */
@Data
public class AIGenerateQuestionRequest implements Serializable {
     /**
     * 应用id
     */
    private Long appId;
    /**
     * 问题数量
     */
    private int questionNum=10;
    /**
     * 选项数量
     */
    private int optionNum=2;
    private static final long serialVersionUID = -1L;
}
