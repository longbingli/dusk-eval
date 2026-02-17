package com.bingli.duskeval.model.dto.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionDTO {

    /**
     * 题目标题
     */
    private String title;

    /**
     * 选项列表
     */
    private List<OptionDTO> options;




    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionDTO {

        /**
         * 测评类答案属性（如 I / E）
         */
        private String result;

        /**
         * 得分类分数
         */
        private Integer score;

        /**
         * 选项内容
         */
        private String value;

        /**
         * 选项标识（A/B/C/D）
         */
        private String key;
    }

}
