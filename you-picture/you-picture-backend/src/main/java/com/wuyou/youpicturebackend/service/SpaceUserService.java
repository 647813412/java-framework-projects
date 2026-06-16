package com.wuyou.youpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuyou.youpicturebackend.model.dto.space.SpaceAddRequest;
import com.wuyou.youpicturebackend.model.dto.space.SpaceQueryRequest;
import com.wuyou.youpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.wuyou.youpicturebackend.model.dto.spaceuser.SpaceUserApplyRequest;
import com.wuyou.youpicturebackend.model.dto.spaceuser.SpaceUserApproveRequest;
import com.wuyou.youpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.wuyou.youpicturebackend.model.entity.Space;
import com.wuyou.youpicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wuyou.youpicturebackend.model.entity.User;
import com.wuyou.youpicturebackend.model.vo.SpaceUserVO;
import com.wuyou.youpicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author xiaofeng
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-12-14 16:50:21
*/
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 添加空间成员
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 校验空间成员
     * @param spaceUser
     * @param add 是否为创建时校验
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 获取空间成员（单个）的方法
     * @param spaceUser
     * @param request
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间成员
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 获取空间成员查询条件
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 用户申请加入团队空间
     */
    long applyJoinSpace(SpaceUserApplyRequest spaceUserApplyRequest, User loginUser);

    /**
     * 审批空间成员申请
     */
    void approveSpaceUser(SpaceUserApproveRequest spaceUserApproveRequest);
}