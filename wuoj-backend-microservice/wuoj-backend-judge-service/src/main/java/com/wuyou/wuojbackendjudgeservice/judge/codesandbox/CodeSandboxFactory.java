package com.wuyou.wuojbackendjudgeservice.judge.codesandbox;


import com.wuyou.wuojbackendjudgeservice.judge.codesandbox.Impl.ExampleCodeSandbox;
import com.wuyou.wuojbackendjudgeservice.judge.codesandbox.Impl.RemoteCodeSandbox;
import com.wuyou.wuojbackendjudgeservice.judge.codesandbox.Impl.ThirdPartyCodeSandbox;

/**
 * 代码沙箱工厂（根据字符串参数创建指定的代码沙箱实例）
 */
public class CodeSandboxFactory {

    /**
     * 创建不同类型的代码沙箱
     * @param type
     * @return
     */
    public static CodeSandbox newInstance(String type) {
        switch (type) {
            case "example":
                return new ExampleCodeSandbox();
            case "remote":
                return new RemoteCodeSandbox();
            case "thirdParty":
                return new ThirdPartyCodeSandbox();
            default:
                return new ExampleCodeSandbox();
        }
    }
}
