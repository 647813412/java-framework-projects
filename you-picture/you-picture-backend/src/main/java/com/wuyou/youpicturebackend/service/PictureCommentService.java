package com.wuyou.youpicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wuyou.youpicturebackend.model.dto.comment.AddCommentRequest;
import com.wuyou.youpicturebackend.model.entity.PictureComment;
import com.wuyou.youpicturebackend.model.entity.User;
import com.wuyou.youpicturebackend.model.vo.CommentVO;

/**
 * 针对表【picture_comment(图片评论)】的数据库操作Service
 */
public interface PictureCommentService extends IService<PictureComment> {

    /**
     * 新增评论
     *
     * @param request   请求参数
     * @param loginUser 当前登录用户
     * @return 评论 VO
     */
    CommentVO addComment(AddCommentRequest request, User loginUser);

    /**
     * 删除评论（本人或管理员）
     *
     * @param commentId 评论ID
     * @param loginUser 当前登录用户
     */
    void deleteComment(Long commentId, User loginUser);

    /**
     * 分页查询某图片的顶级评论，每条附带前 3 条回复
     *
     * @param pictureId 图片ID
     * @param current   当前页
     * @param pageSize  每页大小
     * @return 评论分页
     */
    Page<CommentVO> listTopComments(Long pictureId, long current, long pageSize);

    /**
     * 分页查询某顶级评论的全部回复
     *
     * @param parentId 父评论ID
     * @param current  当前页
     * @param pageSize 每页大小
     * @return 回复分页
     */
    Page<CommentVO> listReplies(Long parentId, long current, long pageSize);
}
