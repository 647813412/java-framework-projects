package com.wuyou.wuojbackendjudgeservice.judge.strategy;


import com.wuyou.wuojbackendmodel.codesandbox.JudgeInfo;

/**
 * 判题策略接口
 */
public interface JudgeStrategy {

    /**
     * 执行判题
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext);
}

