package com.wuyou.youpicturebackend.api.aliyunai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 图片任务查询响应（通用，适用于 wanx2.1-t2i-turbo 和 wanx2.1-imageedit）
 * 与 GetOutPaintingTaskResponse 的区别：结果使用 results[].url 而非 outputImageUrl
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetAiImageTaskResponse {

    /**
     * 请求唯一标识
     */
    private String requestId;

    /**
     * 输出信息
     */
    private Output output;

    @Data
    public static class Output {

        /**
         * 任务 ID
         */
        private String taskId;

        /**
         * 任务状态
         * PENDING / RUNNING / SUCCEEDED / FAILED / CANCELED / UNKNOWN
         */
        private String taskStatus;

        /**
         * 提交时间
         */
        private String submitTime;

        /**
         * 调度时间
         */
        private String scheduledTime;

        /**
         * 结束时间
         */
        private String endTime;

        /**
         * 生成结果列表
         */
        private List<Result> results;

        /**
         * 接口错误码
         */
        private String code;

        /**
         * 接口错误信息
         */
        private String message;

        /**
         * 任务指标信息
         */
        private TaskMetrics taskMetrics;
    }

    @Data
    public static class Result {
        /**
         * 生成图片的 URL
         */
        private String url;
    }

    @Data
    public static class TaskMetrics {
        private Integer total;
        private Integer succeeded;
        private Integer failed;
    }
}
