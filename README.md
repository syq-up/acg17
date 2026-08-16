# ACG17

ACG17 是一个面向个人媒体库的 ACG 资源管理与浏览项目，支持插画、漫画、小说和游戏资源的上传、整理、阅读/预览与回收站管理。

项目采用前后端分离架构：前端负责交互和资源浏览，后端负责用户认证、业务接口、数据库访问以及本地文件存储。

## 功能概览

- 用户登录、JWT 会话认证和用户信息展示
- 插画上传、分页浏览、随机展示、自定义排序和回收站
- 漫画元数据管理、ZIP 章节导入、分页阅读、收藏、分类标签筛选和回收站
- 小说创建、章节新增与编辑、标签、搜索、在线阅读和回收站
- 游戏信息、封面/图标/预览图管理、收藏、随机展示和回收站
- 桌面端与移动端阅读页面
- 上传文件的路径校验、图片格式识别、缩略图生成、短期签名访问和失败文件清理
- 请求参数校验、统一 JSON 响应结构和标准 HTTP 状态码
- 逻辑删除资源；插画和漫画进入回收站后默认保留 14 天，定时任务会自动清理

动画页面目前是前端占位页面，上传功能尚未实现。

## 技术栈

| 部分 | 技术 |
| --- | --- |
| 后端 | Java 25、Spring Boot 4.1、MyBatis-Plus 3.5、MySQL Connector/J |
| 认证 | JWT、BCrypt |
| 前端 | Vue 3、Vue Router、Vuex、Element Plus、Axios |
| 前端构建 | Vite 8、ESLint |
| 数据库 | MySQL 8.4 |
| 本地开发 | Docker Compose |

## 项目结构

```text
.
├── acg17-admin/                         # Spring Boot 后端模块
│   └── src/main/
│       ├── java/com/shiyq/              # Controller、Service、Mapper、实体和工具类
│       └── resources/                    # Spring 配置和 MyBatis XML
├── acg17-ui/                             # Vue 3 前端
│   ├── src/views/                        # 页面和阅读器
│   ├── src/components/                  # 通用组件
│   └── src/util/                        # HTTP 请求封装
├── database/acg17.sql                    # MySQL 表结构和初始化账号
├── compose.yaml                          # 本地 MySQL Compose 配置
├── pom.xml                               # Maven 聚合工程配置
├── .env.example                          # 后端与 Compose 环境变量示例
├── .nvmrc                                # Node.js 版本
└── .sdkmanrc                             # Java 版本
```

## 环境要求

- JDK 25（项目使用 `.sdkmanrc` 中的 `25.0.4-tem`）
- Node.js 24.x（推荐 `.nvmrc` 中的 `24.19.0`）
- npm 11.x（项目锁定的 package manager 为 `npm@11.17.0`）
- libvips 8.15+（后端实时图片派生依赖 ABI 42）
- Docker 和 Docker Compose

## 本地运行

### 1. 配置环境变量和 MySQL

首次运行时复制环境变量文件，并填写数据库、SMTP 和签名密钥：

```bash
cp .env.example .env
chmod 600 .env
```

数据库相关变量如下：

```dotenv
MYSQL_ROOT_PASSWORD=替换为 root 密码
DB_URL=jdbc:mysql://127.0.0.1:3306/acg17
DB_USERNAME=acg17
DB_PASSWORD=替换为 acg17 用户密码
```

启动 MySQL：

```bash
docker compose up -d mysql
docker compose ps
```

Compose 会创建 `acg17` 数据库，并在 MySQL 数据卷首次初始化时导入 [`database/acg17.sql`](database/acg17.sql)。数据库端口只绑定到本机 `127.0.0.1:3306`。

### 2. 启动后端

Ubuntu/WSL2 首次运行前安装 libvips：

```bash
sudo apt-get install -y libvips42t64 libvips-tools
```

公共配置位于 `application.yml`，`application-dev.yml` 和 `application-prod.yml` 只保存端口、上传根目录等环境差异。三个文件都不包含真实凭据，并应正常提交到 Git。

Spring Boot 不会自动读取根目录的 `.env`，启动前需要将其导入当前 shell。JWT 密钥和媒体 URL 签名密钥必须彼此独立，并且都至少包含 32 字节随机数据。

启动开发环境：

```bash
set -a
source .env
set +a
SPRING_PROFILES_ACTIVE=dev ./mvnw -pl acg17-admin spring-boot:run
```

Maven 已为测试和 `spring-boot:run` 配置 libvips FFM 所需的 native access。以后直接运行打包 JAR 时，需要使用 `java --enable-native-access=ALL-UNNAMED -jar ...`。

后端默认地址为 `http://127.0.0.1:18003`，接口上下文路径为 `/api`。

### 3. 配置并启动前端

```bash
cd acg17-ui
npm ci
npm run dev
```

开发服务器默认地址为 [http://127.0.0.1:18001](http://127.0.0.1:18001)。Vite 会把 `/api` 请求代理到 `http://127.0.0.1:18003`。

### 4. 登录

初始化 SQL 会创建本地开发账号：

```text
用户名：admin
密码：admin123
```

该账号仅用于首次本地登录。部署到真实环境前，请立即修改数据库中的密码哈希或移除初始化账号；当前后端未提供注册和修改密码接口。

## 常用命令

在项目根目录执行：

```bash
# 后端测试
./mvnw test

# 后端打包
./mvnw clean package
```

在 `acg17-ui` 目录执行：

```bash
# 代码检查
npm run lint

# 生产构建
npm run build

# 本地预览生产构建
npm run preview
```

生产构建产物位于 `acg17-ui/dist`。部署时需要让前端站点将 `/api` 整体转发到后端，其中包括 `/api/media` 签名媒体接口和 `/api/public-assets/**` 公开装饰素材路径。

## 配置说明

### 后端接口

后端接口统一位于 `/api` 下，主要资源入口如下：

| 入口 | 说明 |
| --- | --- |
| `/api/user` | 登录和退出登录 |
| `/api/user-info` | 当前用户信息 |
| `/api/illustration` | 插画上传、列表、随机展示、排序和回收站 |
| `/api/manga` | 漫画、章节、页面、标签关联、收藏和回收站 |
| `/api/manga-tag` | 漫画分类标签 |
| `/api/novel` | 小说列表、详情、创建和回收站 |
| `/api/novel-chapter` | 小说章节和章节内容 |
| `/api/novel-tag` | 小说标签 |
| `/api/game` | 游戏信息、文件、收藏和回收站 |
| `/api/media` | 校验短期签名后读取用户上传的媒体文件 |
| `/api/public-assets/**` | 无需登录的站点装饰素材 |

> 除登录、随机插画、签名媒体和公开装饰素材接口外，业务接口默认需要在请求头中携带 `Authorization: Bearer <token>`。前端会在登录后自动添加该请求头。

### API 约定

除媒体文件响应外，API 统一返回 JSON：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

`data` 仅在有返回数据时出现，`code` 与 HTTP 响应状态保持一致。查询和普通修改成功返回 `200`，插画上传、漫画/游戏/小说创建以及漫画/小说章节创建返回 `201`。常见错误状态包括：

| HTTP 状态 | 场景 |
| --- | --- |
| `400` | 缺少参数、参数越界、请求体或上传格式错误 |
| `401` | 登录失败，或 Bearer Token 缺失、无效、过期/已失效 |
| `403` | 媒体签名无效或目标路径不允许访问 |
| `404` | 资源不存在、已删除或不属于当前用户 |
| `409` | 资源状态冲突，例如删除仍被漫画引用的标签 |
| `413` | 文件或整个上传请求超限 |
| `415` | 请求内容类型不支持 |
| `429` | 短时间内登录失败次数过多 |
| `500` | 未预期的服务端错误 |

接口按操作语义使用 HTTP 方法：查询使用 `GET`，创建和上传使用 `POST`，更新或从回收站恢复使用 `PUT`，逻辑删除使用 `DELETE`。例如：

```text
DELETE /api/illustration/{id}
PUT    /api/illustration/{id}/restore
DELETE /api/manga/{id}
PUT    /api/manga/{id}/restore
DELETE /api/novel/{id}
PUT    /api/novel/{id}/restore
DELETE /api/game/{id}
PUT    /api/game/{id}/restore
```

漫画不提供手动物理删除接口；删除后先进入回收站，再由定时任务按保留期清理。

### 漫画标签

漫画标签分类如下：

| 分类编号 | 分类名称 |
| --- | --- |
| `1` | 角色 |
| `2` | 男性 |
| `3` | 女性 |
| `4` | 混合 |
| `5` | 其他 |
| `6` | 原作 |
| `7` | 艺术家 |
| `8` | 团队 |

列表页的标签筛选顺序为团队、艺术家、角色、男性、女性、混合、其他、原作。选择任意标签后，前端通过 `tagId` 查询与该标签关联的漫画。

### 文件存储

公共文件配置位于 `acg17-admin/src/main/resources/application.yml`，开发环境上传根目录位于 `application-dev.yml`。主要目录如下：

| 配置项 | 默认值 | 用途 |
| --- | --- | --- |
| `file.uploadFolder` | `/home/shiyq/work-data/acg17/` | 开发环境上传根目录，可由 `FILE_UPLOAD_FOLDER` 覆盖 |
| `file.illustrationFolder` | `illustrations/upload/` | 原图目录 |
| `file.mangaFolder` | `manga/` | 漫画目录 |
| `file.novelFolder` | `novels/` | 小说目录 |
| `file.gameFolder` | `games/` | 游戏目录 |
| `{file.uploadFolder}/media-cache/` | 自动创建 | 实时图片派生缓存，不允许作为签名源文件直接访问 |
| `file.publicAssetFolder` | `illustrations/web-img/` | 允许公开访问的站点装饰素材目录 |
| `file.publicAssetAccessPath` | `/public-assets/**` | 公开装饰素材的访问路径 |

上传限制和 ZIP 解压保护在公共 `application.yml` 中配置：单文件默认 512 MB、单次请求默认 1 GB、单张插画默认 100 MB、单张游戏图片默认 20 MB，一个游戏最多上传 20 张预览图；漫画 ZIP 默认最多 5,000 个条目、单个条目 100 MB、解压后总大小 1 GB。前端上传提示可能比后端限制更严格，以后端配置为准。

用户上传的插画、漫画、游戏文件和头像不再直接映射为静态资源。后端返回的媒体 URL 会绑定规范化后的相对路径和过期时间，并使用 HMAC-SHA256 签名；`/api/media` 只有在签名有效、尚未过期且目标文件位于上传根目录内时才返回文件。签名 URL 在有效期内等同于临时访问凭证，不应写入公开日志或长期保存。

签名媒体 URL 可追加不参与签名的预定义 `style` 参数：省略或 `original` 返回原文件；`small` 和 `medium` 分别仅在图片最长边超过 400px、800px 时保持宽高比缩小。发生缩放时，后端使用 libvips 读取静态图或动图第一帧，生成移除元数据的静态 WebP 并写入 `media-cache`；未知规则和任意宽高参数均不受支持。

### 认证与自动清理

- JWT 默认有效期为 24 小时，可通过 `JWT_EXPIRATION_HOURS` 调整；
- 媒体签名 URL 默认有效期为 60 分钟，可通过 `MEDIA_URL_EXPIRATION_MINUTES` 调整；
- 默认 5 分钟内最多允许 10 次失败登录，可通过 `LOGIN_MAX_ATTEMPTS` 和 `LOGIN_WINDOW_MINUTES` 调整；
- 每天 03:00 清理过期插画，03:10 清理过期漫画，04:00 清理上传残留和待重试删除文件；
- 逻辑删除资源在回收站中保留 14 天，超过期限后由定时任务物理清理。

## 数据库初始化注意事项

`database/acg17.sql` 包含完整表结构以及初始管理员账号。Compose 的初始化脚本只会在 MySQL 数据卷第一次创建时执行。如果修改了 SQL 后需要重新初始化本地数据库，可以在确认不再需要当前本地数据后执行：

```bash
docker compose down -v
docker compose up -d mysql
```

`down -v` 会删除 Compose 管理的 MySQL 数据卷，请谨慎使用。

## 安全提示

- 不要提交 `.env`、`.env.prod`、JWT 密钥、媒体签名密钥、数据库密码、邮件授权码或生产数据；
- `application.yml`、`application-dev.yml` 和 `application-prod.yml` 只包含配置结构和环境变量占位符，应正常提交；
- 生产环境应从部署平台的 Secret、systemd `EnvironmentFile` 或专用密钥管理服务注入变量，不要把本地 `.env.prod` 打包进应用；
- 生产环境应使用彼此独立的高强度随机 JWT 密钥和媒体签名密钥，并将上传目录放在应用目录之外；
- 应修改 SQL 中的默认管理员凭据，并限制数据库和上传文件目录的访问权限；
- 生产部署建议在反向代理层配置 HTTPS、域名和 `/api` 路由转发。
