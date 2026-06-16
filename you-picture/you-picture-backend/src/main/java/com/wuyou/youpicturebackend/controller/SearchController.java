package com.wuyou.youpicturebackend.controller;

import com.wuyou.youpicturebackend.common.BaseResponse;
import com.wuyou.youpicturebackend.common.ResultUtils;
import com.wuyou.youpicturebackend.exception.ErrorCode;
import com.wuyou.youpicturebackend.exception.ThrowUtils;
import com.wuyou.youpicturebackend.model.dto.search.DeleteHistoryRequest;
import com.wuyou.youpicturebackend.model.entity.SearchHistory;
import com.wuyou.youpicturebackend.model.entity.User;
import com.wuyou.youpicturebackend.service.SearchHistoryService;
import com.wuyou.youpicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 搜索历史 & 热门搜索控制器
 */
@RestController
@RequestMapping("/search")
public class SearchController {

    @Resource
    private SearchHistoryService searchHistoryService;

    @Resource
    private UserService userService;

    /**
     * 获取当前用户的搜索历史（最近 20 条，需登录）
     *
     * @param searchType 搜索类型过滤：picture / user / space，不传则返回全部类型
     */
    @GetMapping("/history")
    public BaseResponse<List<SearchHistory>> getHistory(@RequestParam(required = false) String searchType,
                                                        HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<SearchHistory> historyList = searchHistoryService.listHistory(loginUser.getId());
        // 如果指定了类型，过滤
        if (searchType != null && !searchType.isEmpty()) {
            final String type = searchType;
            historyList = historyList.stream()
                    .filter(h -> type.equals(h.getSearchType()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return ResultUtils.success(historyList);
    }

    /**
     * 删除单条搜索历史（需登录）
     */
    @PostMapping("/history/delete")
    public BaseResponse<Boolean> deleteHistory(@RequestBody DeleteHistoryRequest deleteHistoryRequest,
                                               HttpServletRequest request) {
        ThrowUtils.throwIf(deleteHistoryRequest == null || deleteHistoryRequest.getId() == null,
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        searchHistoryService.deleteHistory(deleteHistoryRequest.getId(), loginUser.getId());
        return ResultUtils.success(true);
    }

    /**
     * 清空当前用户全部搜索历史（需登录）
     */
    @PostMapping("/history/clear")
    public BaseResponse<Boolean> clearHistory(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        searchHistoryService.clearHistory(loginUser.getId());
        return ResultUtils.success(true);
    }

    /**
     * 获取热门搜索词 Top 10（无需登录）
     *
     * @param type 搜索类型：picture / user / space
     */
    @GetMapping("/hot")
    public BaseResponse<List<String>> getHotKeywords(@RequestParam(defaultValue = "picture") String type) {
        List<String> hotKeywords = searchHistoryService.getHotKeywords(type);
        return ResultUtils.success(hotKeywords);
    }
}
