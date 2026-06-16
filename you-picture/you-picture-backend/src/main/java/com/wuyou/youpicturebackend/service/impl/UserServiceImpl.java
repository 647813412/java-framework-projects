package com.wuyou.youpicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wuyou.youpicturebackend.exception.BusinessException;
import com.wuyou.youpicturebackend.exception.ErrorCode;
import com.wuyou.youpicturebackend.exception.ThrowUtils;
import com.wuyou.youpicturebackend.manager.auth.StpKit;
import com.wuyou.youpicturebackend.model.constant.UserConstant;
import com.wuyou.youpicturebackend.model.dto.user.UserQueryRequest;
import com.wuyou.youpicturebackend.model.entity.User;
import com.wuyou.youpicturebackend.model.enums.UserRoleEnum;
import com.wuyou.youpicturebackend.model.vo.LoginUserVO;
import com.wuyou.youpicturebackend.model.vo.UserVO;
import com.wuyou.youpicturebackend.service.UserService;
import com.wuyou.youpicturebackend.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.wuyou.youpicturebackend.model.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author xiaofeng
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-11-18 18:13:05
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Value("${security.password.salt:wuyou}")
    private String passwordSalt;

    /**
     * 用户注册
     *
     * @param userAccount   账号
     * @param userPassword  密码
     * @param checkPassword 确认密码
     * @return
     */
    @Override
    public long userRegister(String userName,String userAccount, String userPassword, String checkPassword) {
        //1.校验参数
        if (StrUtil.hasBlank(userName, userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 用户名长度校验
        if (userName.length() < 2 || userName.length() > 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度不合法，需为2-10位");
        }
        // 账号校验：纯数字，长度4-10位
        if (userAccount.length() < 4 || userAccount.length() > 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度需为4-10位");
        }
        // 判断账号是否为纯数字
        if (!userAccount.matches("\\d+")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号必须为纯数字");
        }
        // 密码校验：6-8位，只能包含数字和字母
        if (userPassword.length() < 6 || userPassword.length() > 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度需为6-8位");
        }
        // 判断密码是否为数字和字母的组合（只能包含数字和字母，不能有特殊字符）
        if (!userPassword.matches("^[a-zA-Z0-9]+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码只能包含数字和字母");
        }
        // 两次密码一致性校验
        if (!checkPassword.equals(userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        //2.检查账号和用户名是否重复
        // 检查账号是否重复
        QueryWrapper<User> accountQueryWrapper = new QueryWrapper<>();
        accountQueryWrapper.eq("userAccount", userAccount);
        Long accountCount = this.baseMapper.selectCount(accountQueryWrapper);
        ThrowUtils.throwIf(accountCount > 0, ErrorCode.PARAMS_ERROR, "账号已存在");
        //3.保存数据
        //3.1 密码加密
        String encryptPassword = getEncryptPassword(userPassword);
        //3.2 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(userName);
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean save = this.save(user);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "注册失败，数据库错误");
        //主键返回机制
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验参数
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号错误");
        }
        if (userAccount.length() > 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号错误");
        }
        //获得加密
        String encryptPassword = getEncryptPassword(userPassword);
        //查询数据库，判断是否相等
        QueryWrapper<User> eq = new QueryWrapper<User>().eq("userAccount", userAccount).eq("userPassword", encryptPassword);
        User user = this.getOne(eq);
        //用户不存在
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或者账号密码错误");
        }
        //设置到session中
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        //记录用户登录态到 Sa-token，便于空间鉴权时使用，注意保证该用户信息与 SpringSession 中的信息过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获取当前用户信息
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        //校验
        Object userObject = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObject;
        ThrowUtils.throwIf(currentUser == null ||currentUser.getId() == null ,ErrorCode.NOT_LOGIN_ERROR,"未登录");
        //查询数据库是否存在
        Long userId = currentUser.getId();
        currentUser = this.getById(userId);
        ThrowUtils.throwIf(currentUser == null,ErrorCode.NOT_LOGIN_ERROR);
        return currentUser;
    }

    /**
     * 用户登出
     * @param request
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        //校验用户是否登录
        Object userObject = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObject;
        ThrowUtils.throwIf(currentUser == null ||currentUser.getId() == null ,ErrorCode.NOT_LOGIN_ERROR,"未登录");
        //移除登录信息
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        StpKit.SPACE.logout(currentUser.getId());
        return true;
    }

    /**
     * 获取加密密码
     *
     * @param userPassword
     * @return
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值,混淆密码（通过配置文件注入，支持环境变量覆盖）
        String encryptPassword = DigestUtils.md5DigestAsHex((passwordSalt + userPassword).getBytes());
        return encryptPassword;
    }

    /**
     * 用户脱敏
     *
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 用户脱敏
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 批量脱敏
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
      if (CollectionUtils.isEmpty(userList)){
          return new ArrayList<>();
      }
     return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 获取查询条件
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"请求参数为空");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String searchText = userQueryRequest.getSearchText();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        //组合查询条件
        queryWrapper.eq(Objects.nonNull(id),"id",id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole),"userRole",userRole);
        queryWrapper.like(StrUtil.isNotBlank(userName),"userName",userName);
        queryWrapper.like(StrUtil.isNotBlank(userAccount),"userAccount",userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userProfile),"userProfile",userProfile);
        // searchText 跨账号、用户名、简介做 OR LIKE
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("userAccount", searchText)
                    .or().like("userName", searchText)
                    .or().like("userProfile", searchText));
        }
        //排序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField),sortOrder.equals("ascend"),sortField);
        return queryWrapper;
    }

    /**
     * 是否是管理员
     * @param user
     * @return
     */
    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }
}




