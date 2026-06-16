package com.wuyou.youpicturebackend.model.vo;

import lombok.Data;

/**
 * 点赞响应
 */
@Data
public class LikeVO {

    /**
     * 当前点赞状态（true-已点赞，false-未点赞）
     */
    private Boolean isLiked;

    /**
     * 最新点赞总数
     */
    private Integer likeCount;
}