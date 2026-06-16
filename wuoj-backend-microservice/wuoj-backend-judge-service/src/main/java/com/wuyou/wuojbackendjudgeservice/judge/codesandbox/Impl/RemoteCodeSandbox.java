package com.wuyou.wuojbackendjudgeservice.judge.codesandbox.Impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.wuyou.wuojbackendcommon.common.ErrorCode;
import com.wuyou.wuojbackendcommon.exception.BusinessException;
import com.wuyou.wuojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.wuyou.wuojbackendmodel.codesandbox.ExecuteCodeRequest;
import com.wuyou.wuojbackendmodel.codesandbox.ExecuteCodeResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * 远程代码沙箱
 */
public class RemoteCodeSandbox implements CodeSandbox {
    private static final String AUTH_REQUEST_HEADER="auth";
    private static final String AUTH_REQUEST_SECRET="your-auth-secret";

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("远程代码沙箱");
        String url = "http://your-sandbox-server:8090/executeCode";
        String json = JSONUtil.toJsonStr(executeCodeRequest);
        String responseStr = HttpUtil.createPost(url)
                .header(AUTH_REQUEST_HEADER,AUTH_REQUEST_SECRET)
                .body(json)
                .execute()
                .body();
        if (StringUtils.isBlank(responseStr)){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"executeCode remoteSandbox error,message="+responseStr);
        }
        ExecuteCodeResponse bean = JSONUtil.toBean(responseStr, ExecuteCodeResponse.class);
        System.out.println(bean.toString());
        return JSONUtil.toBean(responseStr, ExecuteCodeResponse.class);
    }
}
