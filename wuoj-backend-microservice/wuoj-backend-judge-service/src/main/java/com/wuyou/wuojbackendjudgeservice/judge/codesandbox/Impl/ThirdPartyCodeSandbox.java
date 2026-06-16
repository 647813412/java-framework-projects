package com.wuyou.wuojbackendjudgeservice.judge.codesandbox.Impl;

import com.wuyou.wuojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.wuyou.wuojbackendmodel.codesandbox.ExecuteCodeRequest;
import com.wuyou.wuojbackendmodel.codesandbox.ExecuteCodeResponse;

/**
 * 第三方代码沙箱
 */
public class ThirdPartyCodeSandbox implements CodeSandbox {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("第三方代码沙箱");
        return null;
    }
}
