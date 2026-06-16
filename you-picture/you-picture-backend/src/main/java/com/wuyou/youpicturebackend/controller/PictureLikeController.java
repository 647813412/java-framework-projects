package com.wuyou.youpicturebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuyou.youpicturebackend.common.BaseResponse;
import com.wuyou.youpicturebackend.common.ResultUtils;
import com.wuyou.youpicturebackend.exception.ErrorCode;
import com.wuyou.youpicturebackend.exception.ThrowUtils;
import com.wuyou.youpicturebackend.model.dto.picture.PictureQueryRequest;
import com.wuyou.youpicturebackend.model.dto.picturelike.LikeRequest;
import com.wuyou.youpicturebackend.model.entity.Picture;
import com.wuyou.youpicturebackend.model.entity.PictureLike;
import com.wuyou.youpicturebackend.model.entity.User;
import com.wuyou.youpicturebackend.model.vo.LikeVO;
import com.wuyou.youpicturebackend.model.vo.PictureVO;
import com.wuyou.youpicturebackend.service.PictureLikeService;
import com.wuyou.youpicturebackend.service.PictureService;
import com.wuyou.youpicturebackend.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/like")
public class PictureLikeController {

    @Resource
    private UserService userService;
    @Resource
    private PictureLikeService pictureLikeService;
    @Resource
    private PictureService pictureService;

    /**
     * 点赞/取消点赞图片
     */
    @PostMapping("/do")
    public BaseResponse<LikeVO> likePicture(@RequestBody @Validated LikeRequest likeRequest,
                                            HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用Service层处理点赞逻辑
        LikeVO likeVO = pictureLikeService.toggleLike(likeRequest.getPictureId(), loginUser.getId());
        return ResultUtils.success(likeVO);
    }

    /**
     * 获取用户是否点赞了某图片
     */
    @GetMapping("/status")
    public BaseResponse<Boolean> getLikeStatus(@RequestParam Long pictureId,
                                               HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 查询点赞状态
        boolean isLiked = pictureLikeService.isLiked(pictureId, loginUser.getId());
        return ResultUtils.success(isLiked);
    }

    /**
     * 获取图片点赞总数
     */
    @GetMapping("/count")
    public BaseResponse<Integer> getLikeCount(@RequestParam Long pictureId) {
        Integer likeCount = pictureLikeService.getLikeCount(pictureId);
        return ResultUtils.success(likeCount);
    }

    /**
     * 获取用户点赞的图片列表
     */
    @PostMapping("/list")
    public BaseResponse<Page<PictureVO>> getLikedPictureList(@RequestBody
                                                                 PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询用户点赞的图片ID
        QueryWrapper<PictureLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", loginUser.getId());
        queryWrapper.orderByDesc("created_at");
        List<PictureLike> likedList = pictureLikeService.list(queryWrapper);
        if (likedList.isEmpty()) {
            return ResultUtils.success(new Page<>(current, size));
        }

        List<Long> pictureIds = likedList.stream()
                .map(PictureLike::getPictureId)
                .collect(Collectors.toList());

        // 分页查询图片
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                new QueryWrapper<Picture>().in("id", pictureIds).orderByDesc("id"));

        // 获取封装类并设置点赞状态为true
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        pictureVOPage.getRecords().forEach(vo -> vo.setIsLiked(true));
        return ResultUtils.success(pictureVOPage);
    }
}
