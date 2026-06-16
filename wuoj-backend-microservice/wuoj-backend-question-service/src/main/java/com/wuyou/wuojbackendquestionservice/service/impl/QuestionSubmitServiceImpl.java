package com.wuyou.wuojbackendquestionservice.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wuyou.wuojbackendcommon.common.ErrorCode;
import com.wuyou.wuojbackendcommon.constant.CommonConstant;
import com.wuyou.wuojbackendcommon.exception.BusinessException;
import com.wuyou.wuojbackendcommon.utils.SqlUtils;
import com.wuyou.wuojbackendmodel.dto.questionsubmit.QuestionSubmitAddRequest;
import com.wuyou.wuojbackendmodel.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.wuyou.wuojbackendmodel.entity.Question;
import com.wuyou.wuojbackendmodel.entity.QuestionSubmit;
import com.wuyou.wuojbackendmodel.entity.User;
import com.wuyou.wuojbackendmodel.enums.QuestionSubmitLanguageEnum;
import com.wuyou.wuojbackendmodel.enums.QuestionSubmitStatusEnum;
import com.wuyou.wuojbackendmodel.vo.QuestionSubmitVO;
import com.wuyou.wuojbackendquestionservice.mapper.QuestionSubmitMapper;
import com.wuyou.wuojbackendquestionservice.rabbitmq.MyMessageProducer;
import com.wuyou.wuojbackendquestionservice.service.QuestionService;
import com.wuyou.wuojbackendquestionservice.service.QuestionSubmitService;
import com.wuyou.wuojbackendserviceclient.service.JudgeFeignClient;
import com.wuyou.wuojbackendserviceclient.service.UserFeignClient;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author xiaofeng
 * @description 针对表【question_submit(题目提交)】的数据库操作Service实现
 * @createDate 2026-01-26 09:46:56
 */
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
        implements QuestionSubmitService {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserFeignClient userFeignClient;
    @Resource
    private JudgeFeignClient judgeFeignClient;
    @Autowired
    private MyMessageProducer myMessageProducer;

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public Long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {
        //1. 校验编程语言是否合法
        String language = questionSubmitAddRequest.getLanguage();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(language);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编程语言错误");
        }
        Long questionId = questionSubmitAddRequest.getQuestionId();
        //2.校验题目是否存在
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        //参数填充
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setUserId(loginUser.getId());
        questionSubmit.setQuestionId(question.getId());
        questionSubmit.setLanguage(language);
        questionSubmit.setCode(questionSubmitAddRequest.getCode());
        // 设置初始状态
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        questionSubmit.setJudgeInfo("{}");
        //todo 每个用户串行提交题目,锁必须要包裹住事务方法
        boolean save = this.save(questionSubmit);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据输入失败");
        }
        Long questionSubmitId = questionSubmit.getId();
        //发送消息
        myMessageProducer.sendMessage("code_exchange","my_routingKey",String.valueOf(questionSubmitId));
//        //执行判题服务,异步的方式
//        CompletableFuture.runAsync(() -> {
//            judgeFeignClient.doJudge(questionSubmitId);
//        });
        return questionSubmitId;
    }

    /**
     * 获取查询条件
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return queryWrapper;
        }
        String language = questionSubmitQueryRequest.getLanguage();
        Integer status = questionSubmitQueryRequest.getStatus();
        Long questionId = questionSubmitQueryRequest.getQuestionId();
        Long userId = questionSubmitQueryRequest.getUserId();
        String sortField = questionSubmitQueryRequest.getSortField();
        String sortOrder = questionSubmitQueryRequest.getSortOrder();

        //拼接查询条件
        queryWrapper.eq(StrUtil.isNotBlank(language), "language", language);
        queryWrapper.eq(ObjectUtil.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(ObjectUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(QuestionSubmitStatusEnum.getEnumByValue(status) != null, "status", status);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        return queryWrapper;
    }

    /**
     * 获取题目封装
     *
     * @param questionSubmit
     * @param loginUser
     * @return
     */
    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser) {
        QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
        // 脱敏：仅本人和管理员能看见自己（提交 userId 和登录用户 id 不同）提交的代码
        long userId = loginUser.getId();
        // 处理脱敏
        if (userId != questionSubmit.getUserId() && !userFeignClient.isAdmin(loginUser)) {
            questionSubmitVO.setCode(null);
        }
        return questionSubmitVO;
    }

    /**
     * 分页获取题目封装
     *
     * @param questionSubmitPage
     * @param loginUser
     * @return
     */
    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser) {
        //获取当前的数据
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVO> questionSubmitVOPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());
        if (CollectionUtils.isEmpty(questionSubmitList)) {
            return questionSubmitVOPage;
        }
        //转换
        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream().map(
                        questionSubmit -> getQuestionSubmitVO(questionSubmit, loginUser))
                .collect(Collectors.toList());
        questionSubmitVOPage.setRecords(questionSubmitVOList);
        return questionSubmitVOPage;
    }
}




