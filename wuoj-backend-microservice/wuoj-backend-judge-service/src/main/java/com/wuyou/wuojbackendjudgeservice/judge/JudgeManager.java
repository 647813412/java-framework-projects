package com.wuyou.wuojbackendjudgeservice.judge;

import com.wuyou.wuojbackendjudgeservice.judge.strategy.DefaultJudgeStrategy;
import com.wuyou.wuojbackendjudgeservice.judge.strategy.JavaLanguageJudgeStrategy;
import com.wuyou.wuojbackendjudgeservice.judge.strategy.JudgeContext;
import com.wuyou.wuojbackendjudgeservice.judge.strategy.JudgeStrategy;
import com.wuyou.wuojbackendmodel.codesandbox.JudgeInfo;
import com.wuyou.wuojbackendmodel.entity.QuestionSubmit;
import org.springframework.stereotype.Service;

@Service
public class JudgeManager {

    public JudgeInfo doJudge(JudgeContext judgeContext) {
        QuestionSubmit questionSubmit = judgeContext.getQuestionSubmit();
        String language = questionSubmit.getLanguage();
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        if (language.equals("java")) {
            judgeStrategy = new JavaLanguageJudgeStrategy();
        }
        return judgeStrategy.doJudge(judgeContext);
    }
}
