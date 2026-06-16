package com.wuyou.wuojbackendjudgeservice.judge.codesandbox;


import com.wuyou.wuojbackendmodel.codesandbox.ExecuteCodeRequest;
import com.wuyou.wuojbackendmodel.codesandbox.ExecuteCodeResponse;

public interface CodeSandbox {

    /**
     * 执行代码
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
