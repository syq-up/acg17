# ACG17

ACG17 是一个面向个人媒体库的 ACG 资源管理与浏览项目，支持插画、漫画、小说和游戏资源的上传、整理、阅读/预览与回收站管理。

项目采用前后端分离架构：前端负责交互和资源浏览，后端负责用户认证、业务接口、数据库访问以及本地文件存储。

## 功能概览

- 用户登录、JWT 会话认证和个人中心；可查看用户名、昵称、加入时间、各类资源统计与小说总字数，并修改昵称、头像和密码（修改密码后当前会话失效）
- 插画上传、按图片比例自适应的画廊、无限滚动浏览、随机展示、拖拽排序、全屏预览和回收站
- 漫画元数据管理、ZIP 章节导入、无限滚动浏览、原标题/中文标题搜索、八类标签多选筛选、随机展示、收藏、章节与页面预览、分页阅读、标签管理和回收站
- 小说创建、章节新增与编辑、关键词搜索、标签筛选、最近添加/字数/更新时间排序、在线阅读和回收站
- 游戏上传（名称、中文名称、版本、简介、封面、图标和预览图）、原标题/中文标题搜索、随机展示和回收站；点击卡片可打开详情弹窗查看封面、原名、版本、简介，并全屏浏览预览图
- 上传文件的路径校验、图片格式识别、实时媒体图片处理、短期签名访问和失败文件清理
- 请求参数校验、统一 JSON 响应结构和标准 HTTP 状态码
- 逻辑删除资源；插画、漫画和游戏进入回收站后默认保留 14 天，定时任务会自动清理

动画页面目前是前端占位页面，上传功能尚未实现。

## 技术栈

| 部分 | 技术 |
| --- | --- |
| 后端 | Java 25、Spring Boot 4.1.0、MyBatis-Plus 3.5.17、MySQL Connector/J |
| 认证 | JWT、BCrypt |
| 前端 | Vue 3.5.41、Vue Router 5.2.0、Vuex 4.1.0、Element Plus 2.14.4、Axios 1.19.0 |
| 前端构建 | Vite 8.2.1、ESLint 10.8.1 |
| 数据库 | MySQL 8.4 |
| 本地开发 | Docker Compose |

## 项目结构

```text
.
├── acg17-admin/                          # Spring Boot 后端模块
│   └── src/main/
│       ├── java/com/shiyq/               # Controller、Service、Mapper、实体和工具类
│       └── resources/                    # Spring 配置和 MyBatis XML
├── acg17-ui/                             # Vue 3 前端
│   ├── src/views/                        # 页面和阅读器
│   ├── src/components/                   # 通用组件
│   └── src/util/                         # HTTP 请求封装
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

该账号仅用于首次本地登录。部署到真实环境前，请立即修改数据库中的密码哈希或移除初始化账号。当前后端未提供注册接口；登录后可在个人中心修改密码，修改成功会使当前会话失效，需要重新登录。

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

生产构建产物位于 `acg17-ui/dist`。部署时需要让前端站点将 `/api` 整体转发到后端，其中包括 `/api/media` 签名媒体接口。

## Docker 镜像部署

仓库提供了生产镜像配置，可在本地或 CI 中构建后端、前端和数据库镜像，将三个镜像导出为单个 tar 文件，再由服务器执行 `docker load` 和 `docker compose up`。服务器只需要 Docker Engine 与 Docker Compose 插件，不需要安装项目的 Java、Node.js、Nginx、libvips 或 MySQL 环境。

完整步骤见 [`deploy/README.md`](deploy/README.md)。

## 配置说明

### 后端接口

后端接口统一位于 `/api` 下，主要资源入口如下：

| 入口 | 说明 |
| --- | --- |
| `/api/user` | 登录和退出登录 |
| `/api/user/password` | 修改密码；修改成功后当前会话失效 |
| `/api/user-info` | 当前用户信息、昵称和头像 |
| `/api/illustration` | 插画上传、列表、随机展示、排序和回收站 |
| `/api/manga` | 漫画、章节、页面、标签关联、收藏和回收站 |
| `/api/manga-tag` | 漫画分类标签 |
| `/api/novel` | 小说列表、详情、创建和回收站 |
| `/api/novel-chapter` | 小说章节和章节内容 |
| `/api/novel-tag` | 小说标签 |
| `/api/game` | 游戏信息、文件、收藏和回收站 |
| `/api/media` | 校验短期签名后读取用户上传的媒体文件 |

> 除登录和签名媒体接口外，业务接口默认需要在请求头中携带 `Authorization: Bearer <token>`。前端会在登录后自动添加该请求头。

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

### 文件存储

公共文件配置位于 `acg17-admin/src/main/resources/application.yml`，开发环境上传根目录位于 `application-dev.yml`。主要目录如下：

| 配置项 | 默认值 | 用途 |
| --- | --- | --- |
| `file.uploadFolder` | `~/data/acg17/` | 开发环境上传根目录，可由 `FILE_UPLOAD_FOLDER` 覆盖 |
| `file.illustrationFolder` | `illustrations/` | 插画目录 |
| `file.mangaFolder` | `manga/` | 漫画目录 |
| `file.novelFolder` | `novels/` | 小说目录 |
| `file.gameFolder` | `games/` | 游戏目录 |
| `{file.uploadFolder}/media-cache/` | 自动创建 | 实时图片派生缓存，不允许作为签名源文件直接访问 |

上传限制和 ZIP 解压保护在公共 `application.yml` 中配置：单文件默认 512 MB、单次请求默认 1 GB、单张插画默认 100 MB、单张游戏图片默认 20 MB，一个游戏最多上传 20 张预览图；漫画 ZIP 默认最多 5,000 个条目、单个条目 100 MB、解压后总大小 1 GB。前端上传提示可能比后端限制更严格，以后端配置为准。

用户上传的插画、漫画、游戏文件和头像不再直接映射为静态资源。后端返回的媒体 URL 会绑定规范化后的相对路径和过期时间，并使用 HMAC-SHA256 签名；`/api/media` 只有在签名有效、尚未过期且目标文件位于上传根目录内时才返回文件。

签名媒体 URL 可追加不参与签名的预定义 `style` 参数：省略或 `original` 返回原文件；`small` 和 `medium` 分别仅在图片最长边超过 450px、900px 时保持宽高比缩小。发生缩放时，后端使用 libvips 读取静态图或动图第一帧，生成移除元数据的静态 WebP 并写入 `media-cache`；未知规则和任意宽高参数均不受支持。

### 认证与自动清理

- JWT 默认有效期为 24 小时，可通过 `JWT_EXPIRATION_HOURS` 调整；
- 媒体签名 URL 默认有效期为 60 分钟，可通过 `MEDIA_URL_EXPIRATION_MINUTES` 调整；
- 默认 5 分钟内最多允许 10 次失败登录，可通过 `LOGIN_MAX_ATTEMPTS` 和 `LOGIN_WINDOW_MINUTES` 调整；
- 每天 03:00 清理过期插画，03:10 清理过期漫画，03:20 清理过期游戏，04:00 清理上传残留和待重试删除文件；
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
