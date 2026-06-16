package com.wuyou.youpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuyou.youpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.wuyou.youpicturebackend.api.aliyunai.model.PictureAutoTagRequest;
import com.wuyou.youpicturebackend.api.aliyunai.model.PictureAutoTagResponse;
import com.wuyou.youpicturebackend.model.dto.picture.*;
import com.wuyou.youpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wuyou.youpicturebackend.model.entity.User;
import com.wuyou.youpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author xiaofeng
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-11-23 16:34:19
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     * @param fileSource
     * @param loginUser
     * @param pictureUploadRequest
     * @return
     */
    PictureVO uploadPicture(Object fileSource, User loginUser, PictureUploadRequest pictureUploadRequest);

    /**
     * 获取图片查询条件
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片（单个）的方法
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage,HttpServletRequest request);

    /**
     * 图片数据校验
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest,User loginUser);

    /**
     * 如果是管理员自动审核，如果是普通用户改成待审核
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture,User loginUser,Long spaceId);

    /**
     * 批量抓取和创建图片
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest  pictureUploadByBatchRequest,User loginUser);

    /**
     * 图片清理
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 删除图片
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 编辑图片
     */
    void editPicture(PictureEditRequest pictureEditRequest,User loginUser);

    /**
     * 空间校验
     * @param loginUser
     * @param spaceId
     */
    void checkPictureAuth(User loginUser, Long spaceId);

    /**
     * 按颜色查询图片
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 批量修改重命名、标签
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建出图任务
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest
            , User loginUser);

    /**
     * 创建文生图任务
     */
    CreateOutPaintingTaskResponse createText2ImageTask(CreateText2ImageRequest createText2ImageRequest, User loginUser);

    /**
     * 创建图像编辑任务
     */
    CreateOutPaintingTaskResponse createImageEditTask(CreateImageEditRequest createImageEditRequest, User loginUser);

    /**
     * AI 智能标签分类
     *
     * @param request   请求参数（pictureId 或 pictureUrl）
     * @param loginUser 登录用户
     * @return AI 推荐的标签和分类
     */
    PictureAutoTagResponse aiAutoTag(PictureAutoTagRequest request, User loginUser);
}