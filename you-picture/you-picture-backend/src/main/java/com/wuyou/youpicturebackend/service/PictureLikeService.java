package com.wuyou.youpicturebackend.service;

import com.wuyou.youpicturebackend.model.entity.PictureLike;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wuyou.youpicturebackend.model.vo.LikeVO;

/**
* @author xiaofeng
* @description 针对表【picture_like(图片点赞记录表)】的数据库操作Service
* @createDate 2026-04-15 23:03:23
*/
public interface PictureLikeService extends IService<PictureLike> {

    /**
     * 点赞/取消点赞图片
     * @param pictureId 图片ID
     * @param userId 用户ID
     * @return 点赞状态和数量
     */
    LikeVO toggleLike(Long pictureId, Long userId);

    /**
     * 判断用户是否点赞了图片
     * @param pictureId 图片ID
     * @param userId 用户ID
     * @return 是否点赞
     */
    boolean isLiked(Long pictureId, Long userId);

    /**
     * 获取图片点赞总数
     * @param pictureId 图片ID
     * @return 点赞总数
     */
    Integer getLikeCount(Long pictureId);

    /**
     * 批量获取用户点赞状态
     * @param pictureIds 图片ID列表
     * @param userId 用户ID
     * @return 点赞状态Map
     */
    java.util.Map<Long, Boolean> batchIsLiked(java.util.List<Long> pictureIds, Long userId);
}
