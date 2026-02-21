package com.bingli.duskeval.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用类型枚举
 *
 * 评分策略（0-自定义，1-AI）
 */
public enum UserScoringStrategyEnum {

     CUSTOM("自定义", 0),
     AI("AI", 1);




    private final String text;

    private final int value;

    UserScoringStrategyEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static UserScoringStrategyEnum getEnumByValue(int value) {
        if (value < 0) {
            return null;
        }
        for (UserScoringStrategyEnum anEnum : UserScoringStrategyEnum.values()) {
            if (anEnum.value == value) {
                return anEnum;
            }
        }
        return null;
    }

    public int getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
