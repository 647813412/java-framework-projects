package com.wuyou.youpicturebackend.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.wuyou.youpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.wuyou.youpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.wuyou.youpicturebackend.api.aliyunai.model.CreateText2ImageTaskRequest;
import com.wuyou.youpicturebackend.api.aliyunai.model.CreateImageEditTaskRequest;
import com.wuyou.youpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.wuyou.youpicturebackend.api.aliyunai.model.GetAiImageTaskResponse;
import com.wuyou.youpicturebackend.api.aliyunai.model.PictureAutoTagResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.wuyou.youpicturebackend.exception.BusinessException;
import com.wuyou.youpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunAiApi {
    // 读取配置文件
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 文生图任务地址
    public static final String CREATE_TEXT2IMAGE_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis";

    // 图像编辑任务地址
    public static final String CREATE_IMAGE_EDIT_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/image-synthesis";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    // 通义千问 VL 多模态接口（用于图片分析）
    public static final String QWEN_VL_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

    /**
     * 创建任务
     *
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        if (createOutPaintingTaskRequest == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "扩图参数为空");
        }
        // 发送请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                // 必须开启异步处理，设置为enable。
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));
        try (HttpResponse httpResponse = httpRequest.execute()) { //会自动关闭资源，Java 7 引入的 try-with-resources 语法
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMessage = response.getMessage();
                log.error("AI 扩图失败，errorCode:{}, errorMessage:{}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图接口响应异常");
            }
            return response;
        }
    }

    /**
     * 查询创建的任务
     *
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 id 不能为空");
        }
        try (HttpResponse httpResponse = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }

    /**
     * 创建文生图任务
     */
    public CreateOutPaintingTaskResponse createText2ImageTask(CreateText2ImageTaskRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文生图参数为空");
        }
        HttpRequest httpRequest = HttpRequest.post(CREATE_TEXT2IMAGE_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(request));
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 文生图失败");
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMessage = response.getMessage();
                log.error("AI 文生图失败，errorCode:{}, errorMessage:{}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 文生图接口响应异常");
            }
            return response;
        }
    }

    /**
     * 创建图像编辑任务
     */
    public CreateOutPaintingTaskResponse createImageEditTask(CreateImageEditTaskRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图像编辑参数为空");
        }
        String requestBody = JSONUtil.toJsonStr(request);
        log.info("图像编辑请求体：{}", requestBody);
        HttpRequest httpRequest = HttpRequest.post(CREATE_IMAGE_EDIT_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(requestBody);
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图像编辑失败");
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMessage = response.getMessage();
                log.error("AI 图像编辑失败，errorCode:{}, errorMessage:{}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图像编辑接口响应异常");
            }
            return response;
        }
    }

    /**
     * 查询 AI 图片任务（文生图 / 图像编辑通用）
     */
    public GetAiImageTaskResponse getAiImageTask(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务 id 不能为空");
        }
        try (HttpResponse httpResponse = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .execute()) {
            if (!httpResponse.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetAiImageTaskResponse.class);
        }
    }

    /**
     * 分析图片内容，智能生成标签和分类
     *
     * @param imageUrl 图片 URL
     * @return AI 推荐的标签和分类
     */
    public PictureAutoTagResponse analyzeImage(String imageUrl) {
        if (StrUtil.isBlank(imageUrl)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片 URL 不能为空");
        }
        // 构造通义千问 VL 请求体
        JSONObject systemTextContent = new JSONObject().set("text",
                "你是一个图片标签分类专家。请分析图片内容，返回JSON格式：" +
                        "{\"tags\":[\"标签1\",\"标签2\",...],\"category\":\"分类名\"}。" +
                        "标签要求：5-8个精准中文标签，涵盖主体、风格、色调、场景、用途等维度。" +
                        "分类要求：根据图片内容自由生成1-3个字的精准分类（例如：风景、人物、美食、科技、动物、建筑、艺术、生活、旅行等）。"  +
                        "分类不要超过3个字，要准确概括图片主题。" +
                        "只返回JSON，不要其他文字。");
        JSONObject systemMessage = new JSONObject()
                .set("role", "system")
                .set("content", new JSONArray().put(systemTextContent));

        JSONObject imageContent = new JSONObject().set("image", imageUrl);
        JSONObject userTextContent = new JSONObject().set("text", "请分析这张图片，给出标签和分类。");
        JSONObject userMessage = new JSONObject()
                .set("role", "user")
                .set("content", new JSONArray().put(imageContent).put(userTextContent));

        JSONObject requestBody = new JSONObject()
                .set("model", "qwen-vl-plus")
                .set("input", new JSONObject()
                        .set("messages", new JSONArray().put(systemMessage).put(userMessage)));

        try (HttpResponse httpResponse = HttpRequest.post(QWEN_VL_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(requestBody.toString())
                .execute()) {
            if (!httpResponse.isOk()) {
                log.error("AI 图片分析请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图片分析失败");
            }
            JSONObject responseJson = JSONUtil.parseObj(httpResponse.body());
            // 检查是否有错误码
            String errorCode = responseJson.getStr("code");
            if (StrUtil.isNotBlank(errorCode)) {
                String errorMessage = responseJson.getStr("message");
                log.error("AI 图片分析失败，errorCode:{}, errorMessage:{}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 图片分析接口响应异常");
            }
            // 解析 AI 返回的内容
            String content = responseJson.getByPath(
                    "output.choices[0].message.content[0].text", String.class);
            if (StrUtil.isBlank(content)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 未返回有效结果");
            }
            // 去除可能的 markdown 代码块标记
            content = content.replace("```json", "").replace("```", "").trim();
            return JSONUtil.toBean(content, PictureAutoTagResponse.class);
        }
    }
}
