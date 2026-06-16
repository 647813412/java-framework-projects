
const fs = require('fs');
const path = 'c:/Users/xiaofeng/Desktop/picture/frontend/src/views/HomeView.vue';
let content = fs.readFileSync(path, 'utf8');

// I also need to add missing ant design icons in the script tag
const iconsImportString = 'import {\n  HeartFilled,\n  ArrowRightOutlined,\n  LeftOutlined,\n  RightOutlined,\n  ClockCircleOutlined,\n  SyncOutlined\n} from \'@ant-design/icons-vue\'';

if (content.indexOf('HeartFilled') !== -1 && content.indexOf('ArrowRightOutlined') === -1) {
  content = content.replace(
    /import \{ HeartFilled \} from '@ant-design\/icons-vue'/g,
    iconsImportString
  );
}

content = content.substring(0, content.indexOf('<template>')) + `
<template>
  <div id=\"homeView\">
    <div class=\"home-banner\">
      <div class=\"banner-bg\"></div>
      
      <div class=\"banner-content\">
        <div class=\"banner-text-section\">
          <h1 class=\"banner-title\">悦木图库小游戏厅上线</h1>
          <p class=\"banner-desc\">
            探索数字影像的极致魅力，让每一像素都焕发生命力。<br />
            悦木图库，为您记录最纯粹的视觉瞬间。
          </p>
          <div class=\"banner-actions\">
            <a-button type=\"primary\" shape=\"round\" class=\"action-btn-primary\" size=\"large\">
              立即开启 <ArrowRightOutlined />
            </a-button>
            <span class=\"banner-time\"><ClockCircleOutlined /> 2026-11-01截止</span>
          </div>
        </div>

        <div class=\"banner-cards-section\">
          <div class=\"card-layer card-back\">
             <img src=\"https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=400&q=80\" alt=\"Mountain\" />
          </div>
          <div class=\"card-layer card-front\">
             <img src=\"https://images.unsplash.com/photo-1560706834-eea14ba84e6f?w=400&q=80\" alt=\"Daisy\" />
             <div class=\"card-nav-btns\">
              <a-button shape=\"circle\" class=\"nav-btn\"><LeftOutlined /></a-button>
              <a-button shape=\"circle\" class=\"nav-btn\"><RightOutlined /></a-button>
             </div>
          </div>
        </div>
        
        <div class=\"banner-indicators\">
          <span class=\"indicator active\"></span>
          <span class=\"indicator\"></span>
          <span class=\"indicator\"></span>
          <span class=\"indicator\"></span>
          <span class=\"indicator\"></span>
        </div>
      </div>
    </div>

    <div class=\"categories-nav\">
      <div class=\"main-tabs\">
        <div class=\"tabs-left\">
          <span class=\"tab-item active\">全部作品</span>
          <span class=\"tab-item\">我的关注</span>
          <span class=\"tab-item\">风云榜单</span>
        </div>
        <div class=\"tabs-center-wave\">
          <svg viewBox=\"0 0 1000 20\" preserveAspectRatio=\"none\">
            <path class=\"wave-line wave-back\" d=\"M0,10 Q250,20 500,10 T1000,10\" fill=\"none\" stroke=\"rgba(0,190,150,0.2)\" stroke-width=\"2\" />
            <path class=\"wave-line wave-front\" d=\"M0,15 Q250,5 500,15 T1000,15\" fill=\"none\" stroke=\"rgba(0,190,150,0.6)\" stroke-width=\"1\" />
          </svg>
        </div>
        <div class=\"tabs-right\">
          <a-button shape=\"round\" class=\"refresh-btn\">
            <SyncOutlined /> 换一换
          </a-button>
        </div>
      </div>

      <div class=\"sub-tags\">
        <span 
          class=\"tag-pill\" 
          @click=\"selectCategory('')\">
          推荐
        </span>
        <span 
          v-for=\"cat in categoryList.slice(0,15)\" 
          :key=\"cat\" 
          class=\"tag-pill\" 
          :class=\"{ active: activeCategory === cat }\" 
          @click=\"selectCategory(cat)\">
          {{ cat }}
        </span>
      </div>
    </div>

    <div class=\"picture-list\">
      <a-spin :spinning=\"loading\" tip=\"拼命加载中...\" size=\"large\">
          <a-empty description=\"暂无符合条件的图片\" />
        </div>

        <div class=\"waterfall-grid\">
          <a-card
            v-for=\"pic in pictures\"
            :key=\"pic.id\"
            class=\"picture-card-wrapper\"
            hoverable
            @click=\"router.push(`/picture/${pic.id}`)\"
          >
            <div class=\"image-wrapper\">
              <img
                :src=\"pic.thumbnailUrl || pic.url\"
                :alt=\"pic.name\"
                loading=\"lazy\"
              />
              <div class=\"overlay-actions\">
                <div class=\"action-top\">
                  <a-button type=\"primary\" shape=\"round\" style=\"background: var(--color-love)\" @click.stop>
                    <HeartFilled /> 收藏
                  </a-button>
                </div>
              </div>
            </div>
            <div class=\"card-footer\">
              <div class=\"flex-between\" style=\"width: 100%\">
                <div class=\"user-info\" v-if=\"pic.user\">
                  <a-avatar :size=\"24\" :src=\"pic.user.userAvatar\" />
                  <span class=\"author-name\">{{ pic.user.userName }}</span>
                </div>
                <div class=\"like-info\">
                  <span>{{ pic.name || '未命名' }}</span>
                </div>
              </div>
            </div>
          </a-card>
        </div>
      </a-spin>
    </div>

    <div class=\"pagination-wrapper\" v-if=\"total > 0\">
      <a-pagination
        v-model:current=\"currentPage\"
        v-model:pageSize=\"pageSize\"
        :total=\"total\"
        show-size-changer
        @change=\"handlePageChange\"
        hideOnSinglePage
      />
    </div>

    <footer class=\"home-footer\">
      <div class=\"footer-bg\"></div>
      <p>© 2026 鹿梦. All rights reserved. 陇ICP备2024012699号-3</p>
    </footer>
  </div>
</template>

<style scoped>
#homeView {
  padding-bottom: 60px;
}

/* Banner 区域 */
.home-banner {
  position: relative;
  width: 100%;
  height: 480px;
  border-radius: 24px;
  overflow: hidden;
  margin-top: 24px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.05);
}
.banner-bg {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: url('https://images.unsplash.com/photo-1444464666168-49b618800ba2?w=1600&q=80') center/cover no-repeat;
  filter: brightness(0.85); 
  z-index: 1;
}
.banner-content {
  position: relative;
  z-index: 2;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 80px;
}
.banner-text-section {
  flex: 1;
  color: #fff;
}
.banner-title {
  font-size: 3.5rem;
  font-weight: bold;
  letter-spacing: 2px;
  margin-bottom: 24px;
  color: #fff;
  line-height: 1.2;
}
.banner-desc {
  font-size: 1.1rem;
  line-height: 1.8;
  opacity: 0.9;
  margin-bottom: 40px;
  max-width: 600px;
}
.banner-actions {
  display: flex;
  align-items: center;
  gap: 24px;
}
.action-btn-primary {
  height: 52px;
  padding: 0 40px;
  font-size: 1.1rem;
  font-weight: 600;
  background: #2f68ff;
  border: none;
  box-shadow: 0 4px 12px rgba(47,104,255,0.4);
}
.banner-time {
  font-size: 1rem;
  color: rgba(255,255,255,0.8);
}

.banner-cards-section {
  position: relative;
  width: 480px;
  height: 320px;
}
.card-layer {
  position: absolute;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0,0,0,0.2);
}
.card-layer img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.card-back {
  width: 200px;
  height: 280px;
  right: 0;
  top: 20px;
  opacity: 0.8;
  transform: scale(0.9) translateX(40px);
}
.card-front {
  width: 280px;
  height: 320px;
  right: 80px;
  top: 0;
  z-index: 3;
}
.card-nav-btns {
  position: absolute;
  bottom: 20px;
  right: 20px;
  display: flex;
  gap: 12px;
}
.nav-btn {
  background: rgba(30,30,30,0.6);
  border: none;
  color: #fff;
}
.nav-btn:hover {
  background: rgba(30,30,30,0.9);
  color: #fff;
}

.banner-indicators {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 12px;
}
.indicator {
  width: 32px;
  height: 4px;
  background: rgba(255,255,255,0.3);
  border-radius: 2px;
  cursor: pointer;
  transition: all 0.3s;
}
.indicator.active {
  background: #fff;
}

/* 导航分类区 */
.categories-nav {
  margin-top: 40px;
}
.main-tabs {
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 16px;
  margin-bottom: 24px;
}
.tabs-left {
  display: flex;
  gap: 32px;
  font-size: 1.1rem;
}
.tab-item {
  color: var(--color-text-secondary);
  font-weight: 500;
  cursor: pointer;
  position: relative;
  transition: color 0.3s;
}
.tab-item:hover {
  color: var(--color-text-primary);
}
.tab-item.active {
  color: var(--color-text-primary);
  font-weight: bold;
}
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -18px;
  left: 0;
  width: 100%;
  height: 3px;
  background: #000;
  border-radius: 2px;
}
.tabs-center-wave {
  flex: 1;
  height: 20px;
  margin: 0 40px;
  position: relative;
}
.wave-line {
  vector-effect: non-scaling-stroke;
}
.refresh-btn {
  color: var(--color-text-secondary);
  border-color: var(--color-border);
  font-size: 0.9rem;
}
.sub-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.tag-pill {
  padding: 6px 20px;
  background: #f5f5f5;
  color: var(--color-text-secondary);
  border-radius: 20px;
  cursor: pointer;
  font-size: 0.95rem;
  transition: all 0.3s;
}
.tag-pill:hover {
  background: #e8e8e8;
}
.tag-pill.active {
  background: #3a3a3a;
  color: #fff;
}

/* 图片列表与瀑布流 */
.picture-list {
  margin-top: 32px;
  min-height: 400px;
}
.waterfall-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 24px;
}

.empty-state {
  padding: 100px 0;
  text-align: center;
}

.picture-card-wrapper {
  overflow: hidden;
  border-radius: 16px;
  transition: transform 0.3s, box-shadow 0.3s;
  border: none;
  background: var(--color-bg-base);
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}
.picture-card-wrapper :deep(.ant-card-body) {
}
.picture-card-wrapper:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.08);
}
.image-wrapper {
  position: relative;
  width: 100%;
  padding-top: 133%; /* 3:4 */
  overflow: hidden;
  background: #f0f0f0; 
}
.image-wrapper img {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}
.picture-card-wrapper:hover .image-wrapper img {
  transform: scale(1.05);
}

.overlay-actions {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: linear-gradient(180deg, rgba(0,0,0,0) 50%, rgba(0,0,0,0.4) 100%);
  opacity: 0;
  visibility: hidden;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 16px;
}
.picture-card-wrapper:hover .overlay-actions {
  opacity: 1;
  visibility: visible;
}
.action-top {
  display: flex;
  justify-content: flex-end;
}

.card-footer {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.flex-between {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.author-name {
  color: var(--color-text-secondary);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.like-info {
  font-weight: 500;
  font-size: 14px;
  color: var(--color-text-primary);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-wrapper {
  text-align: center;
  padding: 40px 0;
}

/* Footer */
.home-footer {
  margin-top: 60px;
  text-align: center;
  position: relative;
  height: 160px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  border-radius: 24px;
  overflow: hidden;
  padding-bottom: 24px;
  color: rgba(255,255,255,0.8);
}
.footer-bg {
  position: absolute;
  bottom: 0; left: 0; right: 0; top: 0;
  background: url('https://images.unsplash.com/photo-1506744012079-c2b64d0dd28d?w=1600&q=80') center bottom/cover no-repeat;
  filter: brightness(0.6);
  z-index: -1;
}
</style>
`

fs.writeFileSync(path, content, 'utf8');

