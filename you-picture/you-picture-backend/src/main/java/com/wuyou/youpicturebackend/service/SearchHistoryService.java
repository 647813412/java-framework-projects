package com.wuyou.youpicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wuyou.youpicturebackend.model.entity.SearchHistory;

import java.util.List;

/**
 * 针对表【search_history(搜索历史)】的数据库操作Service
 */
public interface SearchHistoryService extends IService<SearchHistory> {

    /**
     * 异步记录搜索历史并更新 Redis 热词（仅登录用户且 keyword 不为空时有效）
     *
     * @param userId     用户ID（null 时不记录个人历史，但仍更新热词）
     * @param keyword    搜索关键词
     * @param searchType 搜索类型：picture / user / space
     */
    void recordAsync(Long userId, String keyword, String searchType);

    /**
     * 获取用户搜索历史（最近 20 条，按时间倒序）
     *
     * @param userId 用户ID
     * @return 历史列表
     */
    List<SearchHistory> listHistory(Long userId);

    /**
     * 删除单条搜索历史（仅限本人）
     *
     * @param id     历史记录ID
     * @param userId 当前用户ID
     */
    void deleteHistory(Long id, Long userId);

    /**
     * 清空用户全部搜索历史
     *
     * @param userId 当前用户ID
     */
    void clearHistory(Long userId);

    /**
     * 获取热门搜索词 Top 10（从 Redis ZSet 读取）
     *
     * @param searchType 搜索类型：picture / user / space
     * @return 热门关键词列表（分数从高到低）
     */
    List<String> getHotKeywords(String searchType);
}
