package com.wuyou.youpicturebackend.service.impl;

import java.util.*;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wuyou.youpicturebackend.exception.ErrorCode;
import com.wuyou.youpicturebackend.exception.ThrowUtils;
import com.wuyou.youpicturebackend.exception.BusinessException;
import com.wuyou.youpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.wuyou.youpicturebackend.model.dto.spaceuser.SpaceUserApplyRequest;
import com.wuyou.youpicturebackend.model.dto.spaceuser.SpaceUserApproveRequest;
import com.wuyou.youpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.wuyou.youpicturebackend.model.entity.Space;
import com.wuyou.youpicturebackend.model.entity.SpaceUser;
import com.wuyou.youpicturebackend.model.entity.User;
import com.wuyou.youpicturebackend.model.enums.SpaceRoleEnum;
import com.wuyou.youpicturebackend.model.enums.SpaceTypeEnum;
import com.wuyou.youpicturebackend.model.enums.SpaceUserStatusEnum;
import com.wuyou.youpicturebackend.model.vo.SpaceUserVO;
import com.wuyou.youpicturebackend.model.vo.SpaceVO;
import com.wuyou.youpicturebackend.model.vo.UserVO;
import com.wuyou.youpicturebackend.service.SpaceService;
import com.wuyou.youpicturebackend.service.SpaceUserService;
import com.wuyou.youpicturebackend.mapper.SpaceUserMapper;
import com.wuyou.youpicturebackend.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
 * @author xiaofeng
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2025-12-14 16:50:21
 */
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {

    @Resource
    @Lazy //延迟加载
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    /**
     * 添加空间成员
     */
    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        //参数校验
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtil.copyProperties(spaceUserAddRequest, spaceUser);
        //校验
        validSpaceUser(spaceUser, true);
        //管理员直接添加的成员，状态直接设置为已通过
        spaceUser.setStatus(SpaceUserStatusEnum.APPROVED.getValue());
        //数据库操作
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }

    /**
     * 校验空间成员
     * @param spaceUser
     * @param add 是否为创建时校验
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add) {
            ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            //判断空间是否存在
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            //判断用户是否存在
            ThrowUtils.throwIf(userService.getById(userId) == null, ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        //判断角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        ThrowUtils.throwIf(spaceRole != null && spaceRoleEnum == null, ErrorCode.PARAMS_ERROR, "空间角色不存在");
    }

    /**
     * 获取空间成员（单个）的方法
     * @param spaceUser
     * @param request
     * @return
     */
    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        //对象转换
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        //关联查询用户信息
        Long userId = spaceUser.getUserId();
        if (userId != null && userId >0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceUserVO.setUser(userVO);
        }
        //关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = spaceService.getSpaceVO(space,request);
            spaceUserVO.setSpace(spaceVO);
        }
        return spaceUserVO;
    }

    /**
     * 获取空间成员（列表）的方法
     * @param spaceUserList
     * @return
     */
    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList){
        //判断输入列表是否为空
        if (CollUtil.isEmpty(spaceUserList)){
            return Collections.emptyList();
        }
        //对象转换
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::objToVo).collect(Collectors.toList());
        //收集需要关联查询的用户ID和空间ID
        Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect((Collectors.toSet()));
        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        //批量查询用户和空间
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet)
                        .stream().collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet)
                .stream().collect(Collectors.groupingBy(Space::getId));
        //填充SpaceUserVO的用户和空间信息
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            //填充用户信息
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceUserVO.setUser(userService.getUserVO(user));
            //填充空间信息
            Space space = null;
            if (spaceIdSpaceListMap.containsKey(spaceId)) {
                space = spaceIdSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;
    }

    /**
     * 获取空间成员查询条件
     * @param spaceUserQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        Integer status = spaceUserQueryRequest.getStatus();
        //组合查询
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        // 查询已加入(1)或待审核(0)，忽略传入的status
        queryWrapper.in("status", Arrays.asList(0, 1));
        return queryWrapper;
    }

    /**
     * 用户申请加入团队空间
     */
    @Override
    public long applyJoinSpace(SpaceUserApplyRequest spaceUserApplyRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserApplyRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserApplyRequest.getSpaceId();
        ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
        // 判断空间是否存在
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 判断空间是否为团队空间
        ThrowUtils.throwIf(space.getSpaceType() != SpaceTypeEnum.TEAM.getValue(),
                ErrorCode.PARAMS_ERROR, "只能申请加入团队空间");
        // 判断是否已经申请或已加入
        SpaceUser existSpaceUser = this.lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, loginUser.getId())
                .one();
        if (existSpaceUser != null) {
            if (SpaceUserStatusEnum.APPROVED.getValue() == existSpaceUser.getStatus()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "已是该空间成员");
            }
            if (SpaceUserStatusEnum.PENDING.getValue() == existSpaceUser.getStatus()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "已提交申请，请等待审批");
            }
            // 如果之前被拒绝，允许重新申请
            existSpaceUser.setStatus(SpaceUserStatusEnum.PENDING.getValue());
            boolean result = this.updateById(existSpaceUser);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            return existSpaceUser.getId();
        }
        // 创建申请记录
        SpaceUser spaceUser = new SpaceUser();
        spaceUser.setSpaceId(spaceId);
        spaceUser.setUserId(loginUser.getId());
        spaceUser.setSpaceRole(SpaceRoleEnum.VIEWER.getValue());
        spaceUser.setStatus(SpaceUserStatusEnum.PENDING.getValue());
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }

    /**
     * 审批空间成员申请
     */
    @Override
    public void approveSpaceUser(SpaceUserApproveRequest spaceUserApproveRequest) {
        ThrowUtils.throwIf(spaceUserApproveRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = spaceUserApproveRequest.getId();
        Integer status = spaceUserApproveRequest.getStatus();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        // 状态只能是通过或拒绝
        SpaceUserStatusEnum statusEnum = SpaceUserStatusEnum.getEnumByValue(status);
        ThrowUtils.throwIf(statusEnum == null || statusEnum == SpaceUserStatusEnum.PENDING,
                ErrorCode.PARAMS_ERROR, "审批状态不合法");
        // 查询申请记录
        SpaceUser spaceUser = this.getById(id);
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 只能审批待审批的记录
        ThrowUtils.throwIf(spaceUser.getStatus() != SpaceUserStatusEnum.PENDING.getValue(),
                ErrorCode.OPERATION_ERROR, "该申请已被处理");
        // 更新状态
        spaceUser.setStatus(status);
        boolean result = this.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }
}