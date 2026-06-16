package com.wuyou.youpicturebackend.model.vo;

import com.wuyou.youpicturebackend.model.entity.PictureComment;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 图片评论视图
 */
@Data
public class CommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    private Long id;

    /**
     * 图片ID
     */
    private Long pictureId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 父评论ID（null 表示顶级评论）
     */
    private Long parentId;

    /**
     * 被回复用户ID
     */
    private Long replyUserId;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 评论人信息
     */
    private UserVO userInfo;

    /**
     * 被回复用户信息（仅回复时有值）
     */
    private UserVO replyUserInfo;

    /**
     * 子回复列表（仅顶级评论时返回前 3 条，展开时调用 /reply/list）
     */
    private List<CommentVO> replies;

    /**
     * 子回复总数
     */
    private Integer replyCount;

    /**
     * 从实体转换为 VO（不含用户信息和回复，需调用方填充）
     */
    public static CommentVO fromEntity(PictureComment comment) {
        if (comment == null) {
            return null;
        }
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setPictureId(comment.getPictureId());
        vo.setContent(comment.getContent());
        vo.setParentId(comment.getParentId());
        vo.setReplyUserId(comment.getReplyUserId());
        vo.setLikeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0);
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }
}
