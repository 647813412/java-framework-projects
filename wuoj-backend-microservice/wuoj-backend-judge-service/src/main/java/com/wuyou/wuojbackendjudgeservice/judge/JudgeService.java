package com.wuyou.wuojbackendjudgeservice.judge;

import com.wuyou.wuojbackendmodel.entity.QuestionSubmit;
import org.springframework.stereotype.Service;

@Service
public interface JudgeService {

    QuestionSubmit doJudge(long questionSubmitId);
}
