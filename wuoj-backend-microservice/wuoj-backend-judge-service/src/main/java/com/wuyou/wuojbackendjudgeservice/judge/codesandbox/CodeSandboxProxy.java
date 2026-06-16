package com.wuyou.wuojbackendjudgeservice.judge.codesandbox;

import com.wuyou.wuojbackendmodel.codesandbox.ExecuteCodeRequest;
import com.wuyou.wuojbackendmodel.codesandbox.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CodeSandboxProxy implements CodeSandbox{
    final private CodeSandbox codeSandbox;

    public CodeSandboxProxy(CodeSandbox codeSandbox) {
        this.codeSandbox = codeSandbox;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        log.info("代码沙箱请求信息："+executeCodeRequest.toString());
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(executeCodeRequest);
        log.info("响应"+executeCodeResponse.toString());
        return executeCodeResponse;
    }
}