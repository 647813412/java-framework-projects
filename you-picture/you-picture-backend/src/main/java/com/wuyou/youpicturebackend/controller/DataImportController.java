package com.wuyou.youpicturebackend.controller;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wuyou.youpicturebackend.annotation.AuthCheck;
import com.wuyou.youpicturebackend.common.BaseResponse;
import com.wuyou.youpicturebackend.common.ResultUtils;
import com.wuyou.youpicturebackend.manager.upload.UrlPictureUpload;
import com.wuyou.youpicturebackend.mapper.PictureMapper;
import com.wuyou.youpicturebackend.model.constant.UserConstant;
import com.wuyou.youpicturebackend.model.dto.file.UploadPictureResult;
import com.wuyou.youpicturebackend.model.entity.Picture;
import com.wuyou.youpicturebackend.model.entity.User;
import com.wuyou.youpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 图片数据批量导入接口（管理员专用，完全独立，不依赖现有上传逻辑）
 * 数据来源：
 *   - 静态分类（风景/科技/动物/建筑/美食/太空/海洋/极光/人文/运动/艺术/生活）使用 Picsum 高质量图片
 *   - 动漫分类动态调用 Wallhaven 公开 API 获取 SFW 动漫壁纸
 * 图片均通过 UrlPictureUpload 上传至 COS，数据库存储 COS 地址。
 *
 * 分类（13 个）：风景 / 科技 / 动物 / 建筑 / 美食 / 太空 / 海洋 / 极光 / 人文 / 运动 / 艺术 / 生活 / 动漫
 * 标签（30+）：壁纸, 自然, 高清, 山脉, 晨雾, 夕阳, 森林, 雪山, 草原,
 *              科技, 未来, 电路, 代码, 机器人, 动物, 野生动物, 萌宠, 猫咪,
 *              建筑, 城市, 摩天大楼, 夜景, 美食, 治愈系, 宇宙, 星云, 星空,
 *              海洋, 深海, 极光, 人文, 旅行, 运动, 极限, 艺术, 创意, 二次元
 */
@Slf4j
@RestController
@RequestMapping("/picture/data")
public class DataImportController {

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private UserService userService;

    /** 复用现有 URL 上传组件，下载图片 → 上传 COS → 返回元数据，不修改原代码 */
    @Resource
    private UrlPictureUpload urlPictureUpload;

    /**
     * 批量导入图片数据到公共图库（仅管理员调用一次即可）
     */
    @PostMapping("/import")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> importPictureData(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long adminId = loginUser.getId();
        // 公共图库上传路径前缀，与现有 uploadPicture 逻辑保持一致
        String uploadPathPrefix = String.format("public/%s", adminId);

        List<PictureTask> tasks = new ArrayList<>();
        tasks.addAll(buildStaticTasks());
        tasks.addAll(fetchAnimeTasksFromWallhaven());

        int successCount = 0;
        for (PictureTask task : tasks) {
            try {
                // 通过 UrlPictureUpload 下载原图 → 上传 COS → 获取真实元数据
                UploadPictureResult result = urlPictureUpload.uploadPicture(task.sourceUrl, uploadPathPrefix);

                Picture picture = new Picture();
                // COS 返回的真实地址和元数据
                picture.setUrl(result.getUrl());
                picture.setThumbnailUrl(result.getThumbnailUrl());
                picture.setPicSize(result.getPicSize());
                picture.setPicWidth(result.getPicWidth());
                picture.setPicHeight(result.getPicHeight());
                picture.setPicScale(result.getPicScale());
                picture.setPicFormat(result.getPicFormat());
                picture.setPicColor(result.getPicColor());
                // 业务字段来自静态配置
                picture.setName(task.name);
                picture.setIntroduction(task.introduction);
                picture.setCategory(task.category);
                picture.setTags(JSONUtil.toJsonStr(task.tags));
                picture.setUserId(adminId);
                // 管理员导入，直接通过审核
                picture.setReviewStatus(1);
                picture.setReviewerId(adminId);
                picture.setReviewMessage("管理员批量导入");
                picture.setReviewTime(new Date());
                // 公共图库
                picture.setSpaceId(null);
                picture.setIsPublic(1);
                picture.setIsEdited(0);

                pictureMapper.insert(picture);
                successCount++;
                log.info("导入成功: [{}] → {}", task.name, result.getUrl());
            } catch (Exception e) {
                log.warn("导入失败: name={}, url={}, error={}", task.name, task.sourceUrl, e.getMessage());
            }
        }
        log.info("批量导入完成，共尝试 {} 条，成功 {} 条", tasks.size(), successCount);
        return ResultUtils.success(successCount);
    }

    // ==================== 任务描述 POJO ====================

    /** 描述一张待导入图片的业务信息 + 来源 URL */
    private static class PictureTask {
        String sourceUrl;   // 原始图片 URL（将被下载并上传到 COS）
        String name;
        String introduction;
        String category;
        List<String> tags;

        PictureTask(String sourceUrl, String name, String introduction,
                    String category, List<String> tags) {
            this.sourceUrl = sourceUrl;
            this.name = name;
            this.introduction = introduction;
            this.category = category;
            this.tags = tags;
        }
    }

    private PictureTask task(String url, String name, String intro,
                              String category, String... tags) {
        return new PictureTask(url, name, intro, category, Arrays.asList(tags));
    }

    /** Picsum 1920×1080 图片地址（稳定重定向至真实图片） */
    private String picsum(int id) {
        return "https://picsum.photos/id/" + id + "/1920/1080";
    }

    // ==================== 静态分类任务 ====================

    private List<PictureTask> buildStaticTasks() {
        List<PictureTask> list = new ArrayList<>();

        // ──── 风景 ────
        list.add(task(picsum(15), "高山晨雾",
                "云雾缭绕的山间清晨，阳光穿透薄雾照射在山谷之中，宛如人间仙境。",
                "风景", "壁纸", "自然", "山脉", "晨雾", "高清"));
        list.add(task(picsum(17), "金色夕阳",
                "夕阳西下，天边染上绚丽的橙红色彩，余晖洒在无边原野，温柔而静谧。",
                "风景", "壁纸", "自然", "夕阳", "日落", "高清"));
        list.add(task(picsum(28), "森林秘境",
                "阳光穿透密林，斑驳的光影洒落在松软的落叶地面，静谧而充满生机。",
                "风景", "壁纸", "森林", "自然", "光影", "治愈系"));
        list.add(task(picsum(39), "雪山倒影",
                "皑皑雪山倒映于清澈如镜的湖面，天地之间一片纯净，令人心旷神怡。",
                "风景", "壁纸", "雪山", "湖泊", "倒影", "高清"));
        list.add(task(picsum(56), "翠绿草原",
                "一望无际的绿色草原在微风中轻轻摇曳，远处山峦隐约可见。",
                "风景", "壁纸", "草原", "自然", "绿色", "开阔"));

        // ──── 科技 ────
        list.add(task(picsum(0), "芯片电路",
                "精密的芯片微观世界，纳米级电路交织排列，展现人类工程学的极致智慧。",
                "科技", "科技", "芯片", "电路", "未来", "数字"));
        list.add(task(picsum(2), "代码世界",
                "黑色屏幕上流淌的代码，每一行都是改变世界的语言，程序员的诗意表达。",
                "科技", "科技", "代码", "编程", "数字", "未来"));
        list.add(task(picsum(3), "机械臂",
                "工厂中精准运作的机械臂，工业自动化的核心，人工智能赋予其更多可能。",
                "科技", "科技", "机器人", "工业", "自动化", "未来"));
        list.add(task(picsum(7), "VR 沉浸体验",
                "戴上 VR 头盔，虚拟与现实的边界在此消融，数字世界触手可及。",
                "科技", "科技", "VR", "虚拟现实", "未来", "数字"));

        // ──── 动物 ────
        list.add(task(picsum(169), "雄狮凝视",
                "非洲草原上，雄狮用深邃的眼神凝视远方，威严壮观，百兽之王的气势一览无余。",
                "动物", "动物", "野生动物", "狮子", "自然", "非洲"));
        list.add(task(picsum(219), "慵懒猫咪",
                "阳光下慵懒伸展的橘猫，柔软的毛发金光闪闪，治愈每一个疲惫的瞬间。",
                "动物", "动物", "猫咪", "萌宠", "治愈系", "可爱"));
        list.add(task(picsum(593), "海豚跃水",
                "海豚跃出碧蓝海面的灵动瞬间，自由欢快，充满无限生命力。",
                "动物", "动物", "海豚", "海洋生物", "自然", "自由"));
        list.add(task(picsum(659), "雪地北极熊",
                "北极熊在皑皑白雪中悠然漫步，憨厚可爱的身姿与极地冰雪相映成趣。",
                "动物", "动物", "北极熊", "雪景", "北极", "野生动物"));

        // ──── 建筑 ────
        list.add(task(picsum(19), "现代摩天楼",
                "玻璃幕墙的摩天大楼直插云霄，蓝天映照下格外雄伟，是现代都市的标志。",
                "建筑", "建筑", "摩天大楼", "城市", "现代", "高清"));
        list.add(task(picsum(42), "古堡传说",
                "欧洲中世纪古堡矗立在山头，历史的沧桑与神秘气息扑面而来，令人遐想万千。",
                "建筑", "建筑", "古堡", "欧洲", "历史", "神秘"));
        list.add(task(picsum(96), "夜幕都市",
                "华灯初上的城市夜景，万家灯火汇聚成最温暖的人间图画，繁华而充满活力。",
                "建筑", "建筑", "城市", "夜景", "灯光", "摄影"));
        list.add(task(picsum(110), "东方古韵",
                "飞檐翘角的中式古建筑在青松翠竹间若隐若现，承载着千年文明的精华与智慧。",
                "建筑", "建筑", "中式", "古典", "东方", "历史"));

        // ──── 美食 ────
        list.add(task(picsum(292), "精致早午餐",
                "新鲜水果、全麦吐司与香浓咖啡，精心摆盘的早午餐是美好一天的完美开始。",
                "美食", "美食", "早餐", "生活", "精致", "健康"));
        list.add(task(picsum(431), "日式拉面",
                "金黄浓郁的高汤、劲道的手工面条与嫩滑叉烧，日式拉面的极致鲜美享受。",
                "美食", "美食", "拉面", "日式", "生活", "摄影"));
        list.add(task(picsum(493), "甜品时光",
                "缀满新鲜草莓的奶油蛋糕，入口即化的幸福感，是下午茶时光最甜蜜的陪伴。",
                "美食", "美食", "甜品", "蛋糕", "生活", "治愈系"));

        // ──── 太空 ────
        list.add(task(picsum(177), "星云涌动",
                "数光年之外，彩色星云在宇宙深处静默燃烧，那是恒星诞生与消亡的壮美诗篇。",
                "太空", "太空", "宇宙", "星云", "壁纸", "科幻"));
        list.add(task(picsum(380), "银河深处",
                "璀璨银河横亘苍穹，亿万星辰汇聚成人类仰望最久的那条光带。",
                "太空", "太空", "银河", "星空", "宇宙", "壁纸"));
        list.add(task(picsum(418), "地球轨道",
                "从太空俯瞰那颗蓝色星球，白色云层下隐藏着七十亿人共同的家园。",
                "太空", "太空", "地球", "宇宙", "蓝色星球", "科幻"));
        list.add(task(picsum(442), "星际穿越",
                "宇宙飞船划破黑暗，向未知星系进发，这是人类探索精神的终极浪漫。",
                "太空", "太空", "宇宙", "科幻", "星际", "壁纸"));

        // ──── 海洋 ────
        list.add(task(picsum(241), "珊瑚王国",
                "色彩斑斓的珊瑚礁群落与热带鱼共舞，海底世界的生命奇迹令人叹为观止。",
                "海洋", "海洋", "珊瑚", "深海", "热带鱼", "自然"));
        list.add(task(picsum(273), "深海之光",
                "深海中透过水面折射的幽蓝光芒，寂静而神秘，仿佛另一个宇宙。",
                "海洋", "海洋", "深海", "蓝色", "神秘", "壁纸"));
        list.add(task(picsum(447), "海浪冲击",
                "汹涌的海浪拍打礁石，溅起漫天白色浪花，大海的力量令人震撼与敬畏。",
                "海洋", "海洋", "海浪", "礁石", "壁纸", "自然"));

        // ──── 极光 ────
        list.add(task(picsum(231), "北极光芒",
                "漫天绿色极光在北欧夜空中翻涌跳舞，这是地球最壮观的自然光影奇景。",
                "极光", "极光", "夜景", "北极", "自然奇观", "壁纸"));
        list.add(task(picsum(233), "紫色极光",
                "紫色与绿色交织缠绕的极光点亮冰岛冬夜，如梦似幻，美得令人窒息。",
                "极光", "极光", "冰岛", "夜景", "壁纸", "自然奇观"));
        list.add(task(picsum(332), "极地雪原",
                "皑皑冰雪覆盖的极地旷野，极光映照下散发着迷幻的蓝紫色冷光。",
                "极光", "极光", "雪景", "北极", "冰雪", "壁纸"));

        // ──── 人文 ────
        list.add(task(picsum(64), "街头摄影",
                "城市街头人来人往的瞬间定格，平凡生活中流动着最真实的温度与情感。",
                "人文", "人文", "街头", "摄影", "城市", "生活"));
        list.add(task(picsum(91), "古镇烟雨",
                "烟雨朦胧中的江南古镇，青石板路、乌篷船，时光在此缓缓流淌。",
                "人文", "人文", "古镇", "水乡", "旅行", "中国"));
        list.add(task(picsum(106), "集市一角",
                "热闹集市中的缤纷色彩与人声鼎沸，这是最鲜活的市井烟火气。",
                "人文", "人文", "集市", "生活", "旅行", "摄影"));

        // ──── 运动 ────
        list.add(task(picsum(390), "奔跑的力量",
                "清晨跑道上奔跑的身影，汗水折射出阳光，每一步都是对自我极限的超越。",
                "运动", "运动", "跑步", "健康", "活力", "户外"));
        list.add(task(picsum(464), "冲浪勇士",
                "乘风破浪的冲浪者在巨浪间起舞，与大海的博弈是勇气与自由的完美诠释。",
                "运动", "运动", "冲浪", "海洋", "极限", "活力"));
        list.add(task(picsum(518), "攀岩挑战",
                "徒手攀登垂直岩壁，每一个指尖的发力都承载着不屈的意志与无畏的勇气。",
                "运动", "运动", "攀岩", "极限", "户外", "挑战"));

        // ──── 艺术 ────
        list.add(task(picsum(145), "抽象色彩",
                "流动的色彩在画布上碰撞交融，情感以最直接的形式喷涌，无需解读，只需感受。",
                "艺术", "艺术", "抽象", "色彩", "创意", "美学"));
        list.add(task(picsum(270), "水彩晕染",
                "淡雅的水彩渐变层叠，东方审美的留白之道与西方色彩学的完美融合。",
                "艺术", "艺术", "水彩", "创意", "美学", "治愈系"));
        list.add(task(picsum(417), "几何美学",
                "精确的几何形态在空间中排列组合，极简主义的视觉语言演绎纯粹的美感。",
                "艺术", "艺术", "几何", "极简", "创意", "美学"));

        // ──── 生活 ────
        list.add(task(picsum(136), "咖啡时光",
                "一杯拿铁、一本书，窗边的午后，是城市喧嚣中属于自己的一片宁静。",
                "生活", "生活", "咖啡", "治愈系", "休闲", "文艺"));
        list.add(task(picsum(316), "花海漫步",
                "徜徉在薰衣草花海之中，紫色的芬芳弥漫，世界安静得只剩微风与花香。",
                "生活", "生活", "花卉", "薰衣草", "治愈系", "自然"));
        list.add(task(picsum(366), "雨天窗景",
                "雨滴沿玻璃缓缓滑落，窗外的城市轮廓在雨雾中愈发温柔，是最适合发呆的午后。",
                "生活", "生活", "雨天", "城市", "治愈系", "文艺"));

        return list;
    }

    // ==================== Wallhaven 动漫分类 ====================

    /**
     * 调用 Wallhaven 公开 API 获取 SFW 动漫壁纸，返回任务列表（源 URL + 业务信息）
     * categories=010 表示仅动漫分类，purity=100 表示 SFW
     */
    private List<PictureTask> fetchAnimeTasksFromWallhaven() {
        List<PictureTask> list = new ArrayList<>();
        try {
            String apiUrl = "https://wallhaven.cc/api/v1/search?categories=010&purity=100&sorting=toplist&page=1";
            String response = HttpUtil.get(apiUrl);
            JSONObject json = JSONUtil.parseObj(response);
            JSONArray data = json.getJSONArray("data");
            if (data == null || data.isEmpty()) {
                log.warn("Wallhaven API 返回数据为空，跳过动漫分类导入");
                return list;
            }

            int limit = Math.min(data.size(), 12);
            for (int i = 0; i < limit; i++) {
                try {
                    JSONObject item = data.getJSONObject(i);
                    String imageUrl = item.getStr("path");
                    if (imageUrl == null || imageUrl.isEmpty()) {
                        continue;
                    }
                    // 基础标签 + Wallhaven 原始标签（最多取前 3 个）
                    List<String> tags = new ArrayList<>(Arrays.asList("动漫", "壁纸", "二次元", "高清"));
                    JSONArray wTags = item.getJSONArray("tags");
                    if (wTags != null) {
                        for (int j = 0; j < Math.min(wTags.size(), 3); j++) {
                            String tagName = wTags.getJSONObject(j).getStr("name");
                            if (tagName != null && !tagName.isEmpty()) {
                                tags.add(tagName);
                            }
                        }
                    }
                    list.add(new PictureTask(imageUrl,
                            "动漫壁纸 " + (i + 1),
                            "来自 Wallhaven 的高质量 SFW 动漫壁纸，画面精美，色彩细腻，适合作为桌面壁纸使用。",
                            "动漫", tags));
                } catch (Exception e) {
                    log.warn("解析第 {} 条 Wallhaven 数据失败: {}", i, e.getMessage());
                }
            }
            log.info("Wallhaven 解析到动漫图片任务 {} 条", list.size());
        } catch (Exception e) {
            log.error("调用 Wallhaven API 失败，跳过动漫分类: {}", e.getMessage());
        }
        return list;
    }
}
