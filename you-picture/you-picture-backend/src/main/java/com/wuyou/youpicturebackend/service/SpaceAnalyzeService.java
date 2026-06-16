package com.wuyou.youpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wuyou.youpicturebackend.model.dto.space.SpaceAddRequest;
import com.wuyou.youpicturebackend.model.dto.space.SpaceQueryRequest;
import com.wuyou.youpicturebackend.model.dto.space.analyze.*;
import com.wuyou.youpicturebackend.model.entity.Picture;
import com.wuyou.youpicturebackend.model.entity.Space;
import com.wuyou.youpicturebackend.model.entity.User;
import com.wuyou.youpicturebackend.model.vo.SpaceVO;
import com.wuyou.youpicturebackend.model.vo.space.analyze.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author xiaofeng
*/
public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * 获取空间使用分析数据
     * @param usageAnalyzeRequest
     * @param loginUser
     * @return
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest usageAnalyzeRequest
            ,User loginUser);

    /**
     * 获取空间图片分类分析数据
     * @param categoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest categoryAnalyzeRequest, User loginUser);

    /**
     * 获取空间图片标签使用分析数据
     * @param tagAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest tagAnalyzeRequest, User loginUser);

    /**
     * 获取空间图片大小分析数据
     * @param sizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest sizeAnalyzeRequest, User loginUser);

    /**
     * 获取空间用户上传分析数据
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 获取空间使用排行信息
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}