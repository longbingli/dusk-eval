package com.bingli.duskeval.controller;

import com.bingli.duskeval.annotation.AuthCheck;
import com.bingli.duskeval.common.BaseResponse;
import com.bingli.duskeval.common.ErrorCode;
import com.bingli.duskeval.common.ResultUtils;
import com.bingli.duskeval.common.ReviewRequest;
import com.bingli.duskeval.constant.UserConstant;
import com.bingli.duskeval.exception.BusinessException;
import com.bingli.duskeval.exception.ThrowUtils;
import com.bingli.duskeval.model.entity.App;
import com.bingli.duskeval.model.entity.User;
import com.bingli.duskeval.model.enums.ReviewStatusEnum;
import com.bingli.duskeval.service.AppService;
import com.bingli.duskeval.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

public class ReviewRequestController {


    @Resource
    public AppService appService;

    @Resource
    public UserService userService;

    /**
     * 应用审核
     * @param reviewRequest
     * @param request
     * @return
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doAppReview(@RequestBody ReviewRequest reviewRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(reviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = reviewRequest.getId();
        Integer reviewStatus = reviewRequest.getReviewStatus();
        // 校验
        ReviewStatusEnum reviewStatusEnum = ReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 已是该状态
        if (oldApp.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 更新审核状态
        User loginUser = userService.getLoginUser(request);
        App app = new App();
        app.setId(id);
        app.setReviewStatus(reviewStatus);
        app.setReviewerId(loginUser.getId());
        app.setReviewTime(new Date());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

}
