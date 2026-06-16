package com.wuyou.youpicturebackend.model.dto.user;

import lombok.Data;
import java.io.Serializable;

/**
 * 用户添加类
 */
@Data
public class UserAddRequest implements Serializable {

    private static final long serialVersionUID = 855366102482845790L;
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;
}
