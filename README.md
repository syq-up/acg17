# ACG17

ACG17 是一个面向个人媒体库的 ACG 资源管理与浏览项目，支持插画、漫画、小说和游戏资源的上传、整理、阅读/预览与回收站管理。

项目采用前后端分离架构：前端负责交互和资源浏览，后端负责用户认证、业务接口、数据库访问以及本地文件存储。

## 功能概览

- 用户登录、JWT 会话认证和用户信息展示
- 插画上传、分页浏览、随机展示、自定义排序和回收站
- 漫画元数据管理、封面上传、ZIP 章节导入、分页阅读、收藏、标签和回收站
- 小说创建、章节新增与编辑、标签、搜索、在线阅读和回收站
- 游戏信息、封面/图标/预览图管理、收藏、随机展示和回收站
- 桌面端与移动端阅读页面
- 上传文件的路径校验、图片格式识别、缩略图生成、短期签名访问和失败文件清理
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
├── .env.example                          # Compose 环境变量示例
├── .nvmrc                                # Node.js 版本
└── .sdkmanrc                             # Java 版本
```

## 环境要求

- JDK 25（项目使用 `.sdkmanrc` 中的 `25.0.4-tem`）
- Node.js 24.x（推荐 `.nvmrc` 中的 `24.19.0`）
- npm 11.x（项目锁定的 package manager 为 `npm@11.17.0`）
- Docker 和 Docker Compose

## 本地运行

### 1. 配置 MySQL

复制环境变量文件并填写数据库密码：

```bash
cp .env.example .env
```

`.env` 至少需要包含：

```dotenv
MYSQL_ROOT_PASSWORD=替换为 root 密码
MYSQL_PASSWORD=替换为 acg17 用户密码
```

启动 MySQL：

```bash
docker compose up -d mysql
docker compose ps
```

Compose 会创建 `acg17` 数据库，并在 MySQL 数据卷首次初始化时导入 [`database/acg17.sql`](database/acg17.sql)。数据库端口只绑定到本机 `127.0.0.1:3306`。

### 2. 配置后端

复制开发环境配置：

```bash
cp acg17-admin/src/main/resources/application-dev.yml.example \
   acg17-admin/src/main/resources/application-dev.yml
```

然后编辑 `application-dev.yml`：

- 将 `spring.datasource.password` 改为 `.env` 中的 `MYSQL_PASSWORD`；
- 将 `file.uploadFolder` 改为一个存在且可写的绝对路径；
- 如果启用邮件发送，将 `spring.mail` 下的示例账号和授权码替换为真实配置。

后端必须通过环境变量分别提供 JWT 密钥和媒体 URL 签名密钥，两者都至少包含 32 字节。生产环境应使用彼此独立的随机值。例如：

```bash
export JWT_SECRET='请替换为至少 32 字节的随机字符串'
export MEDIA_URL_SECRET='请替换为另一个至少 32 字节的随机字符串'
```

`.env` 只会被 Docker Compose 自动读取；如果把这两个密钥写入 `.env`，启动 Spring Boot 前仍需将它们导出到当前 shell 环境中。媒体签名默认有效 60 分钟，可通过 `MEDIA_URL_EXPIRATION_MINUTES` 调整。

启动后端：

```bash
./mvnw -pl acg17-admin spring-boot:run
```

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
| `/api/manga` | 漫画、章节、页面、收藏和回收站 |
| `/api/manga-tag` | 漫画标签 |
| `/api/novel` | 小说列表、详情、创建和回收站 |
| `/api/novel-chapter` | 小说章节和章节内容 |
| `/api/novel-tag` | 小说标签 |
| `/api/game` | 游戏信息、文件、收藏和回收站 |
| `/api/media` | 校验短期签名后读取用户上传的媒体文件 |
| `/api/public-assets/**` | 无需登录的站点装饰素材 |

> 除登录、随机插画、签名媒体和公开装饰素材接口外，业务接口默认需要在请求头中携带 `Authorization: Bearer <token>`。前端会在登录后自动添加该请求头。

### 文件存储

默认配置位于 `acg17-admin/src/main/resources/application-dev.yml.example`，主要目录如下：

| 配置项 | 默认值 | 用途 |
| --- | --- | --- |
| `file.uploadFolder` | `/data/acg17/` | 上传根目录 |
| `file.illustrationFolder` | `illustrations/upload/` | 原图目录 |
| `file.illustrationThumbFolder` | `illustrations/upload-t/` | 插画缩略图目录 |
| `file.mangaFolder` | `manga/` | 漫画目录 |
| `file.novelFolder` | `novels/` | 小说目录 |
| `file.gameFolder` | `games/` | 游戏目录 |
| `file.publicAssetFolder` | `illustrations/web-img/` | 允许公开访问的站点装饰素材目录 |
| `file.publicAssetAccessPath` | `/public-assets/**` | 公开装饰素材的访问路径 |

上传限制和 ZIP 解压保护也在该文件中配置：单文件默认 512 MB、单次请求默认 1 GB、单张插画默认 100 MB；漫画 ZIP 默认最多 5,000 个条目、单个条目 100 MB、解压后总大小 1 GB。前端上传提示可能比后端限制更严格，以后端配置为准。

用户上传的插画、漫画、游戏文件和头像不再直接映射为静态资源。后端返回的媒体 URL 会绑定规范化后的相对路径和过期时间，并使用 HMAC-SHA256 签名；`/api/media` 只有在签名有效、尚未过期且目标文件位于上传根目录内时才返回文件。签名 URL 在有效期内等同于临时访问凭证，不应写入公开日志或长期保存。

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

- 不要提交 `.env`、`application-dev.yml`、JWT 密钥、媒体签名密钥、邮件授权码或生产数据；
- 生产环境应使用彼此独立的高强度随机 JWT 密钥和媒体签名密钥，并将上传目录放在应用目录之外；
- 应修改 SQL 中的默认管理员凭据，并限制数据库和上传文件目录的访问权限；
- 生产部署建议在反向代理层配置 HTTPS、域名和 `/api` 路由转发。
