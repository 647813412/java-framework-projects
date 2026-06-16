package com.wuyou.youpicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuyou.youpicturebackend.annotation.AuthCheck;
import com.wuyou.youpicturebackend.common.BaseResponse;
import com.wuyou.youpicturebackend.common.DeleteRequest;
import com.wuyou.youpicturebackend.common.PageRequest;
import com.wuyou.youpicturebackend.common.ResultUtils;
import com.wuyou.youpicturebackend.exception.ErrorCode;
import com.wuyou.youpicturebackend.exception.ThrowUtils;
import com.wuyou.youpicturebackend.model.constant.UserConstant;
import com.wuyou.youpicturebackend.model.dto.user.*;
import com.wuyou.youpicturebackend.model.entity.User;
import com.wuyou.youpicturebackend.model.enums.UserRoleEnum;
import com.wuyou.youpicturebackend.model.vo.LoginUserVO;
import com.wuyou.youpicturebackend.model.vo.UserVO;
import com.wuyou.youpicturebackend.service.SearchHistoryService;
import com.wuyou.youpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private SearchHistoryService searchHistoryService;

    @Value("${security.default-password:12345678}")
    private String defaultPassword;

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> useRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        //参数校验
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.NOT_FOUND_ERROR);
        //账号密码，用户名
        String userName = userRegisterRequest.getUserName();
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userName,userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     * @param userLoginRequest
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        //参数校验
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.NOT_FOUND_ERROR);
        //账号密码
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取用户登录信息
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getUserLogin(HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        //用户脱敏
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户登出
     * @param request
     * @return
     */
    @GetMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request){
        ThrowUtils.throwIf(request ==  null,ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        //用户脱敏
        return ResultUtils.success(result);
    }

    /**
     * 更新用户
     */
    @PostMapping("/user/update")
    @AuthCheck(mustRole = UserConstant.DEFAULT_ROLE)
    public BaseResponse<Boolean> updateUserVO(@RequestBody UserUpdateRequest userUpdateRequest){
        ThrowUtils.throwIf(userUpdateRequest == null || userUpdateRequest.getId() == null ||userUpdateRequest.getId() < 0, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        user.setUserRole(UserConstant.DEFAULT_ROLE);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"更新失败");
        return ResultUtils.success(true);
    }

    /**
     * 根据id获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVoById(long id,HttpServletRequest request){
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);

        String rawId = request.getParameter("id");
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(userService.getUserVO(user));
    }


    /**
     * 用户创建
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest){
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 默认密码（通过配置文件注入，支持环境变量覆盖）
        String encryptPassword = userService.getEncryptPassword(defaultPassword);
        user.setUserPassword(encryptPassword);
        boolean save = userService.save(user);
        ThrowUtils.throwIf(!save,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据id查询用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id){
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }


    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest){
        ThrowUtils.throwIf(deleteRequest == null|| deleteRequest.getId() < 0, ErrorCode.PARAMS_ERROR);
        boolean result = userService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"删除失败");
        return ResultUtils.success(result);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest){
        ThrowUtils.throwIf(userUpdateRequest == null || userUpdateRequest.getId() == null ||userUpdateRequest.getId() < 0, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"更新失败");
        return ResultUtils.success(true);
    }


    /**
     * 分页获取用户封装列表（仅管理员）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest){
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize), userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 用户搜索（公开接口，支持按账号、用户名、简介模糊搜索）
     */
    @PostMapping("/search")
    public BaseResponse<Page<UserVO>> searchUser(@RequestBody UserQueryRequest userQueryRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        Page<User> userPage = userService.page(new Page<>(current, pageSize), userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        userVOPage.setRecords(userService.getUserVOList(userPage.getRecords()));
        // 异步记录搜索历史
        String searchText = userQueryRequest.getSearchText();
        if (cn.hutool.core.util.StrUtil.isNotBlank(searchText)) {
            Long userId = null;
            try {
                userId = userService.getLoginUser(request).getId();
            } catch (Exception ignored) {
            }
            searchHistoryService.recordAsync(userId, searchText, "user");
        }
        return ResultUtils.success(userVOPage);
    }
}