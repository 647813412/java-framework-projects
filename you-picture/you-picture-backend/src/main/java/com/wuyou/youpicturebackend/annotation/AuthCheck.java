package com.wuyou.youpicturebackend.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {
    /**
     * 必须要有某个角色
     * @return
     */
    String mustRole() default "";
}