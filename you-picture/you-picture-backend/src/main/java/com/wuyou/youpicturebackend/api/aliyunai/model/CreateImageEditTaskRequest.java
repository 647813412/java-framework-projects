package com.wuyou.youpicturebackend.api.aliyunai.model;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.io.Serializable;

/**
 * 图像编辑任务请求（wanx2.1-imageedit）
 */
@Data
public class CreateImageEditTaskRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模型名称
     */
    private String model = "wanx2.1-imageedit";

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
         * 必选，编辑功能类型
         * description_edit: 指令编辑
         * expand: 扩图
         * stylization_all: 全局风格化
         * remove_text_watermark: 去文字水印
         * super_resolution: 图像超分
         */
        private String function;

        /**
         * 必选，文本提示词
         */
        private String prompt;

        /**
         * 必选，原图 URL
         */
        @Alias("base_image_url")
        private String baseImageUrl;
    }

    @Data
    public static class Parameters implements Serializable {
        /**
         * 可选，生成图片的数量，默认 1
         */
        private Integer n = 1;

        /**
         * 可选，是否开启 prompt 智能改写
         * 默认关闭，保持用户原始 prompt 不被改写
         */
        @Alias("prompt_extend")
        private Boolean promptExtend = false;
    }
}
