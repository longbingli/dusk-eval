package com.bingli.duskeval.scoring;


import com.bingli.duskeval.model.entity.App;
import com.bingli.duskeval.model.entity.UserAnswer;

import java.util.List;

public interface ScoringStrategy {

    /**
     * 计算分数
     * @param choices
     * @param app
     * @return
     */
    UserAnswer doScore(List<String> choices, App  app)throws Exception;
}
