package com.wuyou.wuojbackendjudgeservice.judge;

import cn.hutool.json.JSONUtil;
import com.wuyou.wuojbackendcommon.common.ErrorCode;
import com.wuyou.wuojbackendcommon.exception.BusinessException;
import com.wuyou.wuojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.wuyou.wuojbackendjudgeservice.judge.codesandbox.CodeSandboxFactory;
import com.wuyou.wuojbackendjudgeservice.judge.codesandbox.CodeSandboxProxy;
import com.wuyou.wuojbackendjudgeservice.judge.strategy.JudgeContext;
import com.wuyou.wuojbackendmodel.codesandbox.ExecuteCodeRequest;
import com.wuyou.wuojbackendmodel.codesandbox.ExecuteCodeResponse;
import com.wuyou.wuojbackendmodel.codesandbox.JudgeInfo;
import com.wuyou.wuojbackendmodel.dto.question.JudgeCase;
import com.wuyou.wuojbackendmodel.entity.Question;
import com.wuyou.wuojbackendmodel.entity.QuestionSubmit;
import com.wuyou.wuojbackendmodel.enums.QuestionSubmitStatusEnum;
import com.wuyou.wuojbackendserviceclient.service.QuestionFeignClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionFeignClient questionFeignClient;
    @Value("${codesandbox.type}")
    private String type;
    @Resource
    private JudgeManager judgeManager;

    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        //根据id，查找提交信息

        QuestionSubmit questionSubmit = questionFeignClient.getQuestionSubmitById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        //根据提交信息，找到对应题目信息，查看是否存在
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionFeignClient.getQuestionById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }

        //查看题目状态是否是等待中,如果不为等待状态，抛出异常
        Integer status = questionSubmit.getStatus();
        if (!QuestionSubmitStatusEnum.WAITING.getValue().equals(status)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在判题");
        }

        //更改题目状态为"判题中"，防止重复执行
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
        boolean update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目状态更新失败");
        }

        //调用沙箱
        CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
        codeSandbox = new CodeSandboxProxy(codeSandbox);
        //封装请求
        String code = questionSubmit.getCode();
        String language = questionSubmit.getLanguage();
        String judgeCaseStr = question.getJudgeCase();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        ExecuteCodeRequest executeCodeRequest = ExecuteCodeRequest.builder().code(code).language(language).inputList(inputList).build();

        //接受沙箱返回结果
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);

        //进行判题
        JudgeContext judgeContext = new JudgeContext();
        judgeContext.setInputList(inputList);
        judgeContext.setOutputList(executeCodeResponse.getOutputList());
        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo() );
        judgeContext.setJudgeCaseList(judgeCaseList);
        judgeContext.setQuestion(question);
        judgeContext.setQuestionSubmit(questionSubmit);
        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);

        //修改数据库中的判题结果
        questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        update = questionFeignClient.updateQuestionSubmitById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新失败");
        }
        return questionFeignClient.getQuestionSubmitById(questionId);
    }
}
