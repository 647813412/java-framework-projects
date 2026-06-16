package com.wuyou.wuojbackendjudgeservice.judge.strategy;

import com.wuyou.wuojbackendmodel.codesandbox.JudgeInfo;
import com.wuyou.wuojbackendmodel.dto.question.JudgeCase;
import com.wuyou.wuojbackendmodel.entity.Question;
import com.wuyou.wuojbackendmodel.entity.QuestionSubmit;
import lombok.Data;

import java.util.List;

@Data
public class JudgeContext {
    private List<String> inputList;

    private List<String> outputList;

    private JudgeInfo judgeInfo;

    private List<JudgeCase> judgeCaseList;

    private Question question;

    private QuestionSubmit questionSubmit;
}
