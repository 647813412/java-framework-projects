package com.wuyou.wuojbackendjudgeservice.judge.codesandbox.Impl;

import com.wuyou.wuojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.wuyou.wuojbackendmodel.codesandbox.ExecuteCodeRequest;
import com.wuyou.wuojbackendmodel.codesandbox.ExecuteCodeResponse;
import com.wuyou.wuojbackendmodel.codesandbox.JudgeInfo;
import com.wuyou.wuojbackendmodel.enums.JudgeInfoMessageEnum;
import com.wuyou.wuojbackendmodel.enums.QuestionSubmitStatusEnum;

import java.util.List;

/**
 * 示例代码沙箱
 */
public class ExampleCodeSandbox implements CodeSandbox {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(inputList);
        executeCodeResponse.setMessage("测试执行成功");
        executeCodeResponse.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(JudgeInfoMessageEnum.ACCEPTED.getText());
        judgeInfo.setMemory(100L);
        judgeInfo.setTime(100L);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }
}
