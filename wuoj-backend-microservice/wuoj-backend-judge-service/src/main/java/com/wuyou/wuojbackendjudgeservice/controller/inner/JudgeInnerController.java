package com.wuyou.wuojbackendjudgeservice.controller.inner;

import com.wuyou.wuojbackendjudgeservice.judge.JudgeService;
import com.wuyou.wuojbackendmodel.entity.QuestionSubmit;
import com.wuyou.wuojbackendserviceclient.service.JudgeFeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/inner")
public class JudgeInnerController implements JudgeFeignClient {
    @Resource
    private JudgeService judgeService;

    @Override
    @GetMapping("/do")
    public QuestionSubmit doJudge(@RequestParam("questionSubmitId")  long questionSubmitId) {
        return judgeService.doJudge(questionSubmitId);
    }
}
