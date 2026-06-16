package com.wuyou.youpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wuyou.youpicturebackend.model.dto.user.UserQueryRequest;
import com.wuyou.youpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wuyou.youpicturebackend.model.vo.LoginUserVO;
import com.wuyou.youpicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author xiaofeng
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-11-18 18:13:06
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userAccount 账号
     * @param userPassword 密码
     * @param checkPassword 确认密码
     * @return
     */
    long userRegister(String userName,String userAccount,String userPassword,String checkPassword);

    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @return
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户登出
     * @param request
     * @return
     */
    boolean userLogout (HttpServletRequest request);

    /**
     * 得到加密的密码
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户脱敏
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户脱敏
     * @param useer
     * @return
     */
    UserVO getUserVO(User useer);

    /**
     * 批量获取用户脱敏
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询条件
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     *是否是管理员
     * @param user
     * @return
     */
    boolean isAdmin(User user);
}
