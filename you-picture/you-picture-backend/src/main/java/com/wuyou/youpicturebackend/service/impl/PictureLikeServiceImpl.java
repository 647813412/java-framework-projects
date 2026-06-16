package com.wuyou.youpicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wuyou.youpicturebackend.exception.ErrorCode;
import com.wuyou.youpicturebackend.exception.ThrowUtils;
import com.wuyou.youpicturebackend.model.entity.Picture;
import com.wuyou.youpicturebackend.model.entity.PictureLike;
import com.wuyou.youpicturebackend.model.vo.LikeVO;
import com.wuyou.youpicturebackend.service.PictureLikeService;
import com.wuyou.youpicturebackend.mapper.PictureLikeMapper;
import com.wuyou.youpicturebackend.service.PictureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author xiaofeng
* @description 针对表【picture_like(图片点赞记录表)】的数据库操作Service实现
* @createDate 2026-04-15 23:03:23
*/
@Service
public class PictureLikeServiceImpl extends ServiceImpl<PictureLikeMapper, PictureLike>
    implements PictureLikeService{

    @Autowired
    private PictureService pictureService;

    /**
     * 点赞/取消点赞图片
     * @param pictureId 图片ID
     * @param userId 用户ID
     * @return 点赞状态和数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LikeVO toggleLike(Long pictureId, Long userId) {
        // 1. 校验图片是否存在
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 2. 查询是否已点赞
        QueryWrapper<PictureLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("picture_id", pictureId);
        queryWrapper.eq("user_id", userId);
        PictureLike existingLike = this.getOne(queryWrapper);

        boolean isLiked;
        int likeCountChange;

        if (existingLike != null) {
            // 3. 已点赞 -> 取消点赞
            this.removeById(existingLike.getId());
            isLiked = false;
            likeCountChange = -1;
        } else {
            // 4. 未点赞 -> 添加点赞
            PictureLike pictureLike = new PictureLike();
            pictureLike.setPictureId(pictureId);
            pictureLike.setUserId(userId);
            pictureLike.setCreatedAt(LocalDateTime.now());
            this.save(pictureLike);
            isLiked = true;
            likeCountChange = 1;
        }

        // 5. 更新图片表中的点赞总数
        Integer newLikeCount = picture.getLikeCount() + likeCountChange;
        if (newLikeCount < 0) {
            newLikeCount = 0;
        }
        picture.setLikeCount(newLikeCount);
        pictureService.updateById(picture);

        // 6. 返回结果
        LikeVO likeVO = new LikeVO();
        likeVO.setIsLiked(isLiked);
        likeVO.setLikeCount(newLikeCount);

        return likeVO;
    }

    /**
     * 判断用户是否点赞了图片
     * @param pictureId 图片ID
     * @param userId 用户ID
     * @return 是否点赞
     */
    @Override
    public boolean isLiked(Long pictureId, Long userId) {
        QueryWrapper<PictureLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("picture_id", pictureId);
        queryWrapper.eq("user_id", userId);
        return this.count(queryWrapper) > 0;
    }

    /**
     * 获取图片点赞总数
     * @param pictureId 图片ID
     * @return 点赞总数
     */
    @Override
    public Integer getLikeCount(Long pictureId) {
        QueryWrapper<PictureLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("picture_id", pictureId);
        return Math.toIntExact(this.count(queryWrapper));
    }

    /**
     * 批量获取用户点赞状态
     * @param pictureIds 图片ID列表
     * @param userId 用户ID
     * @return 点赞状态Map
     */
    @Override
    public Map<Long, Boolean> batchIsLiked(List<Long> pictureIds, Long userId) {
        if (pictureIds == null || pictureIds.isEmpty() || userId == null) {
            return Collections.emptyMap();
        }

        // 查询用户点赞的图片
        QueryWrapper<PictureLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.in("picture_id", pictureIds);
        List<PictureLike> likedList = this.list(queryWrapper);

        Set<Long> likedPictureIds = likedList.stream()
                .map(PictureLike::getPictureId)
                .collect(Collectors.toSet());

        // 构建返回Map
        return pictureIds.stream()
                .collect(Collectors.toMap(
                        pictureId -> pictureId,
                        likedPictureIds::contains
                ));
    }

}




