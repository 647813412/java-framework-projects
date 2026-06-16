package com.wuyou.wuojbackenduserservice.controller.inner;

import com.wuyou.wuojbackendmodel.entity.User;
import com.wuyou.wuojbackendserviceclient.service.UserFeignClient;
import com.wuyou.wuojbackenduserservice.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;


/**
 * 该服务仅内部调用
 */
@RestController
@RequestMapping("/inner")
public class UserInnerController implements UserFeignClient {

    @Resource
    private UserService userService;

    @GetMapping("/get/id")
    @Override
    public User getById(@RequestParam("userId") Long userId) {
        return userService.getById(userId);
    }

    @GetMapping("/get/ids")
    @Override
    public List<User> listByIds(@RequestParam("idList") Collection<Long> idList) {
        return userService.listByIds(idList);
    }
}
