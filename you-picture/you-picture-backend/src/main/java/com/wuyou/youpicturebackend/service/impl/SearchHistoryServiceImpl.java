package com.wuyou.youpicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wuyou.youpicturebackend.exception.ErrorCode;
import com.wuyou.youpicturebackend.exception.ThrowUtils;
import com.wuyou.youpicturebackend.mapper.SearchHistoryMapper;
import com.wuyou.youpicturebackend.model.entity.SearchHistory;
import com.wuyou.youpicturebackend.service.SearchHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 搜索历史 Service 实现
 */
@Slf4j
@Service
public class SearchHistoryServiceImpl extends ServiceImpl<SearchHistoryMapper, SearchHistory>
        implements SearchHistoryService {

    /**
     * Redis 热词 ZSet key 前缀，完整 key = HOT_KEY_PREFIX + searchType
     */
    private static final String HOT_KEY_PREFIX = "search:hot:";

    /**
     * 每个用户保留的最大历史条数
     */
    private static final int MAX_HISTORY_SIZE = 20;

    /**
     * 热词 Top N
     */
    private static final int HOT_TOP_N = 10;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void recordAsync(Long userId, String keyword, String searchType) {
        if (StrUtil.isBlank(keyword) || StrUtil.isBlank(searchType)) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 更新 Redis 热词（不依赖登录状态）
                String hotKey = HOT_KEY_PREFIX + searchType;
                stringRedisTemplate.opsForZSet().incrementScore(hotKey, keyword, 1);
                // 2. 记录个人历史（需登录）
                if (userId == null) {
                    return;
                }
                // 避免连续重复记录同一关键词
                LambdaQueryWrapper<SearchHistory> existWrapper = new LambdaQueryWrapper<>();
                existWrapper.eq(SearchHistory::getUserId, userId)
                        .eq(SearchHistory::getKeyword, keyword)
                        .eq(SearchHistory::getSearchType, searchType);
                long existCount = this.count(existWrapper);
                if (existCount > 0) {
                    // 更新时间：删除旧的再插入
                    this.remove(existWrapper);
                }
                // 插入新记录
                SearchHistory history = new SearchHistory();
                history.setUserId(userId);
                history.setKeyword(keyword);
                history.setSearchType(searchType);
                this.save(history);
                // 超过上限时删除最旧的
                LambdaQueryWrapper<SearchHistory> countWrapper = new LambdaQueryWrapper<>();
                countWrapper.eq(SearchHistory::getUserId, userId)
                        .eq(SearchHistory::getSearchType, searchType)
                        .orderByAsc(SearchHistory::getCreateTime);
                List<SearchHistory> allHistory = this.list(countWrapper);
                if (allHistory.size() > MAX_HISTORY_SIZE) {
                    int deleteCount = allHistory.size() - MAX_HISTORY_SIZE;
                    List<Long> idsToDelete = allHistory.subList(0, deleteCount)
                            .stream().map(SearchHistory::getId).collect(Collectors.toList());
                    this.removeByIds(idsToDelete);
                }
            } catch (Exception e) {
                log.warn("记录搜索历史失败: keyword={}, type={}, error={}", keyword, searchType, e.getMessage());
            }
        });
    }

    @Override
    public List<SearchHistory> listHistory(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        LambdaQueryWrapper<SearchHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SearchHistory::getUserId, userId)
                .orderByDesc(SearchHistory::getCreateTime)
                .last("LIMIT " + MAX_HISTORY_SIZE);
        return this.list(wrapper);
    }

    @Override
    public void deleteHistory(Long id, Long userId) {
        ThrowUtils.throwIf(id == null || userId == null, ErrorCode.PARAMS_ERROR);
        SearchHistory history = this.getById(id);
        ThrowUtils.throwIf(history == null, ErrorCode.NOT_FOUND_ERROR, "历史记录不存在");
        ThrowUtils.throwIf(!history.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR);
        this.removeById(id);
    }

    @Override
    public void clearHistory(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        LambdaQueryWrapper<SearchHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SearchHistory::getUserId, userId);
        this.remove(wrapper);
    }

    @Override
    public List<String> getHotKeywords(String searchType) {
        if (StrUtil.isBlank(searchType)) {
            return Collections.emptyList();
        }
        String hotKey = HOT_KEY_PREFIX + searchType;
        // 按分数从高到低取 Top N
        Set<String> keywords = stringRedisTemplate.opsForZSet()
                .reverseRange(hotKey, 0, HOT_TOP_N - 1);
        if (keywords == null) {
            return Collections.emptyList();
        }
        return keywords.stream().collect(Collectors.toList());
    }
}




