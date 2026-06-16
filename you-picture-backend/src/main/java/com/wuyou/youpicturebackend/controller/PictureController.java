package com.wuyou.youpicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wuyou.youpicturebackend.annotation.AuthCheck;
import com.wuyou.youpicturebackend.api.aliyunai.AliYunAiApi;
import com.wuyou.youpicturebackend.api.aliyunai.model.*;
import com.wuyou.youpicturebackend.api.imagesearch.model.SoImageSearchResult;
import com.wuyou.youpicturebackend.api.imagesearch.sub.GetSoImageListApi;
import com.wuyou.youpicturebackend.api.imagesearch.sub.SoImageSearchApiFacade;
import com.wuyou.youpicturebackend.common.BaseResponse;
import com.wuyou.youpicturebackend.common.DeleteRequest;
import com.wuyou.youpicturebackend.common.ResultUtils;
import com.wuyou.youpicturebackend.exception.BusinessException;
import com.wuyou.youpicturebackend.exception.ErrorCode;
import com.wuyou.youpicturebackend.exception.ThrowUtils;
import com.wuyou.youpicturebackend.manager.auth.SpaceUserAuthManager;
import com.wuyou.youpicturebackend.manager.auth.StpKit;
import com.wuyou.youpicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.wuyou.youpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.wuyou.youpicturebackend.model.constant.UserConstant;
import com.wuyou.youpicturebackend.model.dto.picture.*;
import com.wuyou.youpicturebackend.model.entity.Picture;
import com.wuyou.youpicturebackend.model.entity.Space;
import com.wuyou.youpicturebackend.model.entity.User;
import com.wuyou.youpicturebackend.model.enums.PictureReviewStatusEnum;
import com.wuyou.youpicturebackend.model.vo.PictureTagCategory;
import com.wuyou.youpicturebackend.model.vo.PictureVO;
import com.wuyou.youpicturebackend.service.PictureService;
import com.wuyou.youpicturebackend.service.SearchHistoryService;
import com.wuyou.youpicturebackend.service.SpaceService;
import com.wuyou.youpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();
    @Resource
    private SpaceService spaceService;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private SearchHistoryService searchHistoryService;

    /**
     * 上传图片（可重新上传）
     *
     * @param multipartFile
     * @return
     */
    @PostMapping("/upload")
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 HttpServletRequest request,
                                                 PictureUploadRequest pictureUploadRequest) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, loginUser, pictureUploadRequest);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 通过URL上传图片
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadUrlPicture(@RequestBody PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        String fileUrl = pictureUploadRequest.getFileUrl();
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, loginUser, pictureUploadRequest);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        pictureService.deletePicture(id, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        //空间的图片，需要校验权限,编程式校验
        Space space = null;
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        }
        //获取权限列表
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
//        使用权限框架校验
//        if (spaceId != null) {
//            User loginUser = userService.getLoginUser(request);
//            pictureService.checkPictureAuth(loginUser, spaceId);
//        }
        // 获取封装类
        return ResultUtils.success(pictureVO);
    }


    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        //空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        //公开图库
        if (spaceId == null) {
            // 获取当前登录用户
            User loginUser = null;
            try {
                loginUser = userService.getLoginUser(request);
            } catch (Exception e) {
                // 未登录用户，保持为 null
            }
            // 判断是否是查看个人主页（可以通过请求参数区分，例如传入 userId）
            Long targetUserId = pictureQueryRequest.getUserId();

            // 如果是用户查看自己的个人主页
            if (targetUserId != null && loginUser != null && targetUserId.equals(loginUser.getId())) {
                // 查看自己的主页：不限制审核状态，可以看到自己的所有图片（包括未审核的）
                pictureQueryRequest.setReviewStatus(null); // 不筛选审核状态
                pictureQueryRequest.setNullSpaceId(true);
                pictureQueryRequest.setIsPublic(1);
            } else {
                // 公开图库或查看他人主页：普通用户默认只能看到已审核的数据
                pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
                pictureQueryRequest.setNullSpaceId(true);
                pictureQueryRequest.setIsPublic(1);
            }
        } else {
            //私有空间
//            User loginUser = userService.getLoginUser(request);
//            Space space = spaceService.getById(spaceId);
//            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
//            if (!loginUser.getId().equals(space.getUserId())) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
//            }
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        }
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 异步记录搜索历史（仅当有关键词且用户已登录）
        String searchText = pictureQueryRequest.getSearchText();
        if (cn.hutool.core.util.StrUtil.isNotBlank(searchText)) {
            final User finalLoginUser;
            try {
                finalLoginUser = userService.getLoginUser(request);
            } catch (Exception e) {
                // 未登录，仅更新热词
                searchHistoryService.recordAsync(null, searchText, "picture");
                return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
            }
            searchHistoryService.recordAsync(finalLoginUser.getId(), searchText, "picture");
        }
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }



    /**
     * 缓存接口
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWtihCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                                      HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        //普通用户默认只能看到已审核的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        //先查询缓存
        //1. 构建key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        //利用哈希算法(md5)压缩
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String redisKey = "yunpicture:listPictureVOByPage:" + hashKey;
        //从本地缓存查询
        String localCacheValue = LOCAL_CACHE.getIfPresent(redisKey);
        if (localCacheValue != null) {
            //缓存命中，返回结果
            Page<PictureVO> cachedPage = JSONUtil.toBean(localCacheValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        //2. 从redis中查询缓存
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        String cachedValue = valueOps.get(redisKey);
        if (cachedValue != null) {
            LOCAL_CACHE.put(cachedValue, redisKey);
            //缓存命中，返回结果
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        //转换成JSON字符串格式
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        //存入本地缓存
        LOCAL_CACHE.put(cacheValue, redisKey);
        //5-10 分钟随机过期，防止雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        valueOps.set(redisKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
        //返回结果
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 审核图片
     *
     * @param pictureReviewRequest
     * @param request
     * @return
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }


    /**
     * 展示分类/标签（从图片表动态聚合，按出现次数降序）
     *
     * @return
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        // 只查公开已审核的图片的 category 和 tags 两列
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.wuyou.youpicturebackend.model.entity.Picture> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.select("category", "tags")
                .eq("reviewStatus", com.wuyou.youpicturebackend.model.enums.PictureReviewStatusEnum.PASS.getValue())
                .eq("isPublic", 1)
                .isNull("spaceId");
        List<com.wuyou.youpicturebackend.model.entity.Picture> pictures = pictureService.list(wrapper);

        // 统计分类出现次数，按频次降序
        Map<String, Long> categoryCount = new HashMap<>();
        // 统计标签出现次数，按频次降序
        Map<String, Long> tagCount = new HashMap<>();

        for (com.wuyou.youpicturebackend.model.entity.Picture picture : pictures) {
            // 聚合分类
            String category = picture.getCategory();
            if (StrUtil.isNotBlank(category)) {
                categoryCount.merge(category.trim(), 1L, Long::sum);
            }
            // 解析 tags JSON 数组并聚合
            String tagsJson = picture.getTags();
            if (StrUtil.isNotBlank(tagsJson)) {
                try {
                    List<String> tags = JSONUtil.toList(tagsJson, String.class);
                    for (String tag : tags) {
                        if (StrUtil.isNotBlank(tag)) {
                            tagCount.merge(tag.trim(), 1L, Long::sum);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        List<String> categoryList = categoryCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<String> tagList = tagCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        pictureTagCategory.setCategoryList(categoryList);
        pictureTagCategory.setTagList(tagList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 批量上传
     *
     * @param pictureUploadByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 以图搜图
     */
    @PostMapping("/search/picture/so")
    public BaseResponse<List<SoImageSearchResult>> searchPictureByPictureIsSo(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        List<SoImageSearchResult> resultList = new ArrayList<>();
        // 这个 start 是控制查询多少页, 每页是 20 条
        int start = 0;
        while (resultList.size() <= 50) {
            List<SoImageSearchResult> tempList = SoImageSearchApiFacade.searchImage(oldPicture.getUrl(), start);
            if (tempList.isEmpty()) {
                break;
            }
            resultList.addAll(tempList);
            start += tempList.size();
        }
        return ResultUtils.success(resultList);
    }

    /**
     * 颜色搜图
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        String color = searchPictureByColorRequest.getPicColor();
        String picColor = "#" + color;
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> result = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        System.out.println(result);
        return ResultUtils.success(result);
    }

    /**
     * 批量修改重命名、标签
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 创建 AI 扩图任务
     */
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    @PostMapping("/out_painting/create_task")
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(@RequestBody
                                                                                    CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
                                                                                    HttpServletRequest request) {
        ThrowUtils.throwIf(createPictureOutPaintingTaskRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse result = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        System.out.println(result.getOutput().getTaskId());
        return ResultUtils.success(result);
    }


    /**
     * 更新图片（仅管理员可用）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,
                                               HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (pictureUpdateRequest == null || loginUser == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        picture.setIsPublic(1);
        // 数据校验
        pictureService.validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //补充审核参数
        pictureService.fillReviewParams(picture, loginUser,null);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }



    /**
     * 查询AI扩图任务
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        System.out.println(taskId);
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }

    // region 文生图

    /**
     * 创建文生图任务
     */
    @PostMapping("/text2image/create_task")
    public BaseResponse<CreateOutPaintingTaskResponse> createText2ImageTask(@RequestBody CreateText2ImageRequest createText2ImageRequest,
                                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(createText2ImageRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse result = pictureService.createText2ImageTask(createText2ImageRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 查询文生图任务
     */
    @GetMapping("/text2image/get_task")
    public BaseResponse<GetAiImageTaskResponse> getText2ImageTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetAiImageTaskResponse task = aliYunAiApi.getAiImageTask(taskId);
        return ResultUtils.success(task);
    }
    // endregion

    // region AI 智能标签

    /**
     * AI 智能标签分类（根据图片内容自动生成标签和分类）
     */
    @PostMapping("/auto_tag")
    public BaseResponse<PictureAutoTagResponse> aiAutoTag(@RequestBody PictureAutoTagRequest request,
                                                          HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        PictureAutoTagResponse response = pictureService.aiAutoTag(request, loginUser);
        return ResultUtils.success(response);
    }

    // endregion

}
