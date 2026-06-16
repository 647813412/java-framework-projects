package com.wuyou.youpicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wuyou.youpicturebackend.exception.BusinessException;
import com.wuyou.youpicturebackend.exception.ErrorCode;
import com.wuyou.youpicturebackend.exception.ThrowUtils;
import com.wuyou.youpicturebackend.mapper.PictureCommentMapper;
import com.wuyou.youpicturebackend.model.dto.comment.AddCommentRequest;
import com.wuyou.youpicturebackend.model.entity.PictureComment;
import com.wuyou.youpicturebackend.model.entity.User;
import com.wuyou.youpicturebackend.model.vo.CommentVO;
import com.wuyou.youpicturebackend.model.vo.UserVO;
import com.wuyou.youpicturebackend.service.PictureCommentService;
import com.wuyou.youpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图片评论 Service 实现
 */
@Service
public class PictureCommentServiceImpl extends ServiceImpl<PictureCommentMapper, PictureComment>
        implements PictureCommentService {

    @Resource
    private UserService userService;

    @Override
    public CommentVO addComment(AddCommentRequest request, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request.getPictureId() == null, ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getContent()), ErrorCode.PARAMS_ERROR, "评论内容不能为空");
        ThrowUtils.throwIf(request.getContent().length() > 500, ErrorCode.PARAMS_ERROR, "评论内容不能超过500字");
        // 如果是回复，校验父评论存在且本身是顶级评论
        if (request.getParentId() != null) {
            PictureComment parent = this.getById(request.getParentId());
            ThrowUtils.throwIf(parent == null, ErrorCode.NOT_FOUND_ERROR, "父评论不存在");
            ThrowUtils.throwIf(parent.getParentId() != null, ErrorCode.PARAMS_ERROR, "不支持三级及以上嵌套评论");
        }
        // 构建评论实体
        PictureComment comment = new PictureComment();
        comment.setPictureId(request.getPictureId());
        comment.setUserId(loginUser.getId());
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        comment.setReplyUserId(request.getReplyUserId());
        comment.setLikeCount(0);
        this.save(comment);
        // 填充用户信息后返回
        return buildCommentVO(comment);
    }

    @Override
    public void deleteComment(Long commentId, User loginUser) {
        ThrowUtils.throwIf(commentId == null, ErrorCode.PARAMS_ERROR);
        PictureComment comment = this.getById(commentId);
        ThrowUtils.throwIf(comment == null, ErrorCode.NOT_FOUND_ERROR, "评论不存在");
        // 只允许本人或管理员删除
        boolean isOwner = comment.getUserId().equals(loginUser.getId());
        boolean isAdmin = userService.isAdmin(loginUser);
        ThrowUtils.throwIf(!isOwner && !isAdmin, ErrorCode.NO_AUTH_ERROR, "无权删除此评论");
        // 如果是顶级评论，同时删除所有子回复
        if (comment.getParentId() == null) {
            LambdaQueryWrapper<PictureComment> childWrapper = new LambdaQueryWrapper<>();
            childWrapper.eq(PictureComment::getParentId, commentId);
            this.remove(childWrapper);
        }
        this.removeById(commentId);
    }

    @Override
    public Page<CommentVO> listTopComments(Long pictureId, long current, long pageSize) {
        ThrowUtils.throwIf(pictureId == null, ErrorCode.PARAMS_ERROR);
        // 查询顶级评论（parentId IS NULL）
        LambdaQueryWrapper<PictureComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PictureComment::getPictureId, pictureId)
                .isNull(PictureComment::getParentId)
                .orderByDesc(PictureComment::getCreateTime);
        Page<PictureComment> entityPage = this.page(new Page<>(current, pageSize), wrapper);
        // 转换为 VO
        List<PictureComment> records = entityPage.getRecords();
        List<CommentVO> voList = fillCommentVOList(records, true);
        Page<CommentVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public Page<CommentVO> listReplies(Long parentId, long current, long pageSize) {
        ThrowUtils.throwIf(parentId == null, ErrorCode.PARAMS_ERROR);
        LambdaQueryWrapper<PictureComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PictureComment::getParentId, parentId)
                .orderByAsc(PictureComment::getCreateTime);
        Page<PictureComment> entityPage = this.page(new Page<>(current, pageSize), wrapper);
        List<CommentVO> voList = fillCommentVOList(entityPage.getRecords(), false);
        Page<CommentVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ======================== 私有方法 ========================

    /**
     * 将评论实体列表转为 VO，批量填充用户信息，顶级评论附带前3条回复
     */
    private List<CommentVO> fillCommentVOList(List<PictureComment> comments, boolean withReplies) {
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }
        // 收集所有需要查询的用户 ID
        Set<Long> userIds = comments.stream().map(PictureComment::getUserId).collect(Collectors.toSet());
        Set<Long> replyUserIds = comments.stream()
                .filter(c -> c.getReplyUserId() != null)
                .map(PictureComment::getReplyUserId)
                .collect(Collectors.toSet());
        userIds.addAll(replyUserIds);
        // 批量查用户
        Map<Long, UserVO> userMap = batchGetUserVOMap(userIds);
        // 顶级评论 ID 列表（用于查子回复）
        List<Long> commentIds = comments.stream().map(PictureComment::getId).collect(Collectors.toList());
        // 批量查各顶级评论的子回复（前3条 + 总数）
        Map<Long, List<PictureComment>> replyMap = Collections.emptyMap();
        Map<Long, Long> replyCountMap = Collections.emptyMap();
        if (withReplies && !commentIds.isEmpty()) {
            // 查全部子回复
            LambdaQueryWrapper<PictureComment> replyWrapper = new LambdaQueryWrapper<>();
            replyWrapper.in(PictureComment::getParentId, commentIds)
                    .orderByAsc(PictureComment::getCreateTime);
            List<PictureComment> allReplies = this.list(replyWrapper);
            replyMap = allReplies.stream().collect(Collectors.groupingBy(PictureComment::getParentId));
            // 计算每个顶级评论的回复总数
            final Map<Long, List<PictureComment>> finalReplyMap = replyMap;
            replyCountMap = commentIds.stream()
                    .collect(Collectors.toMap(id -> id, id -> (long) finalReplyMap.getOrDefault(id, Collections.emptyList()).size()));
            // 子回复也需要填充用户信息
            for (PictureComment reply : allReplies) {
                userIds.add(reply.getUserId());
                if (reply.getReplyUserId() != null) {
                    userIds.add(reply.getReplyUserId());
                }
            }
            userMap = batchGetUserVOMap(userIds);
        }
        // 组装 VO
        final Map<Long, List<PictureComment>> finalReplyMap2 = replyMap;
        final Map<Long, Long> finalReplyCountMap = replyCountMap;
        final Map<Long, UserVO> finalUserMap = userMap;
        return comments.stream().map(c -> {
            CommentVO vo = CommentVO.fromEntity(c);
            vo.setUserInfo(finalUserMap.get(c.getUserId()));
            if (c.getReplyUserId() != null) {
                vo.setReplyUserInfo(finalUserMap.get(c.getReplyUserId()));
            }
            if (withReplies) {
                List<PictureComment> replies = finalReplyMap2.getOrDefault(c.getId(), Collections.emptyList());
                // 前 3 条
                List<CommentVO> replyVOs = replies.stream().limit(3).map(r -> {
                    CommentVO rVo = CommentVO.fromEntity(r);
                    rVo.setUserInfo(finalUserMap.get(r.getUserId()));
                    if (r.getReplyUserId() != null) {
                        rVo.setReplyUserInfo(finalUserMap.get(r.getReplyUserId()));
                    }
                    return rVo;
                }).collect(Collectors.toList());
                vo.setReplies(replyVOs);
                vo.setReplyCount(replies.size());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 批量获取用户 VO map，key = userId
     */
    private Map<Long, UserVO> batchGetUserVOMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userService.listByIds(userIds).stream().collect(Collectors.toMap(
                User::getId,
                user -> {
                    UserVO vo = new UserVO();
                    BeanUtils.copyProperties(user, vo);
                    return vo;
                }
        ));
    }

    /**
     * 单条评论转 VO（含用户信息，无回复列表）
     */
    private CommentVO buildCommentVO(PictureComment comment) {
        CommentVO vo = CommentVO.fromEntity(comment);
        User user = userService.getById(comment.getUserId());
        if (user != null) {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            vo.setUserInfo(userVO);
        }
        return vo;
    }
}




