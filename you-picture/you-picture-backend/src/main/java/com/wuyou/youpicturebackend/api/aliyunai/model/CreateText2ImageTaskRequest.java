package com.wuyou.youpicturebackend.api.aliyunai.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 文生图任务请求（wanx2.1-t2i-turbo）
 */
@Data
public class CreateText2ImageTaskRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模型名称
     */
    private String model = "wanx2.1-t2i-turbo";

    /**
     * 输入信息
     */
    private Input input;

    /**
     * 图像处理参数
     */
    private Parameters parameters;

    @Data
    public static class Input implements Serializable {
        /**
         * 必选，文本提示词
         */
        private String prompt;
    }

    @Data
    public static class Parameters implements Serializable {
        /**
         * 可选，生成图片的分辨率，默认 1024*1024
         */
        private String size = "1024*1024";

        /**
         * 可选，生成图片的数量，默认 1
         */
        private Integer n = 1;
    }
}
