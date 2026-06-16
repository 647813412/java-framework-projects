package com.wuyou.youpicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuyou.youpicturebackend.common.BaseResponse;
import com.wuyou.youpicturebackend.common.ResultUtils;
import com.wuyou.youpicturebackend.exception.ErrorCode;
import com.wuyou.youpicturebackend.exception.ThrowUtils;
import com.wuyou.youpicturebackend.model.dto.comment.AddCommentRequest;
import com.wuyou.youpicturebackend.model.dto.comment.DeleteCommentRequest;
import com.wuyou.youpicturebackend.model.entity.User;
import com.wuyou.youpicturebackend.model.vo.CommentVO;
import com.wuyou.youpicturebackend.service.PictureCommentService;
import com.wuyou.youpicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 图片评论控制器
 */
@RestController
@RequestMapping("/comment")
public class PictureCommentController {

    @Resource
    private PictureCommentService pictureCommentService;

    @Resource
    private UserService userService;

    /**
     * 发表评论（需登录）
     */
    @PostMapping("/add")
    public BaseResponse<CommentVO> addComment(@RequestBody AddCommentRequest addCommentRequest,
                                              HttpServletRequest request) {
        ThrowUtils.throwIf(addCommentRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        CommentVO commentVO = pictureCommentService.addComment(addCommentRequest, loginUser);
        return ResultUtils.success(commentVO);
    }

    /**
     * 删除评论（本人或管理员）
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteComment(@RequestBody DeleteCommentRequest deleteCommentRequest,
                                               HttpServletRequest request) {
        ThrowUtils.throwIf(deleteCommentRequest == null || deleteCommentRequest.getId() == null,
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureCommentService.deleteComment(deleteCommentRequest.getId(), loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 分页查询图片的顶级评论（每条附带前3条回复）
     *
     * @param pictureId 图片ID
     * @param current   当前页，默认 1
     * @param pageSize  每页大小，默认 10，最大 50
     */
    @GetMapping("/list")
    public BaseResponse<Page<CommentVO>> listComments(@RequestParam Long pictureId,
                                                      @RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long pageSize) {
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        pageSize = Math.min(pageSize, 50);
        Page<CommentVO> page = pictureCommentService.listTopComments(pictureId, current, pageSize);
        return ResultUtils.success(page);
    }

    /**
     * 展开查看某条顶级评论的全部回复
     *
     * @param parentId 父评论ID
     * @param current  当前页，默认 1
     * @param pageSize 每页大小，默认 20，最大 50
     */
    @GetMapping("/reply/list")
    public BaseResponse<Page<CommentVO>> listReplies(@RequestParam Long parentId,
                                                     @RequestParam(defaultValue = "1") long current,
                                                     @RequestParam(defaultValue = "20") long pageSize) {
        ThrowUtils.throwIf(parentId == null || parentId <= 0, ErrorCode.PARAMS_ERROR);
        pageSize = Math.min(pageSize, 50);
        Page<CommentVO> page = pictureCommentService.listReplies(parentId, current, pageSize);
        return ResultUtils.success(page);
    }
}
