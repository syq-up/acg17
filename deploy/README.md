# ACG17 Docker 部署

在本地构建并导出镜像，服务器只负责 `docker load` 和 `docker compose up`。服务器无需安装 Java、Node.js、Maven、Nginx、MySQL 或 libvips，只需安装 Docker Engine 和 Docker Compose 插件。

- 对外地址：`http://服务器IP:18000`
- 媒体目录：宿主机 `/data/acg17`
- 默认构建架构：ARM64

## 1. 本地构建

在项目根目录执行。`IMAGE_TAG` 是版本号，构建和部署时必须一致。

ARM64（默认，适用于 `aarch64`/`arm64` 服务器）：

```bash
IMAGE_TAG=0.1.0 ./deploy/build-images.sh
```

也可以显式指定：

```bash
TARGET_PLATFORM=linux/arm64 IMAGE_TAG=0.1.0 ./deploy/build-images.sh
```

x86_64（适用于 `x86_64`/AMD64 服务器）：

```bash
TARGET_PLATFORM=linux/amd64 IMAGE_TAG=0.1.0 ./deploy/build-images.sh
```

生成的镜像包位于 `deploy/out/`：

```text
acg17-images-0.1.0-linux-arm64.tar
acg17-images-0.1.0-linux-amd64.tar
```

每个包只支持对应的 CPU 架构，可在服务器上用 `uname -m` 确认架构。

## 2. 准备服务器文件

上传以下文件到服务器同一目录：

```text
acg17-images-0.1.0-linux-arm64.tar   # 或 amd64 包
compose.prod.yaml
.env
```

`.env` 以 `deploy/.env.example` 为模板。至少修改所有密码、邮件凭据和两个签名密钥：

```bash
cp deploy/.env.example deploy/.env
openssl rand -hex 32
openssl rand -hex 32
```

关键配置如下：

```dotenv
IMAGE_TAG='0.1.0'
HTTP_BIND_ADDRESS='0.0.0.0'
HTTP_PORT='18000'
MEDIA_DATA_DIR='/data/acg17'
```

两个随机值分别填入 `JWT_SECRET` 和 `MEDIA_URL_SECRET`，不要使用相同值。

## 3. 授权媒体目录

后端以 UID/GID `10001:10001` 运行。推荐保留旧文件所有者，仅通过 ACL 授权：

```bash
sudo setfacl -R -m u:10001:rwX /data/acg17
sudo setfacl -R -d -m u:10001:rwX /data/acg17
```

如果该目录只供 ACG17 使用，也可以改所有者：

```bash
sudo chown -R 10001:10001 /data/acg17
sudo chmod -R u+rwX /data/acg17
```

旧媒体路径必须与旧数据库记录匹配。只有媒体文件而没有对应数据库数据时，系统不会自动导入这些文件。

## 4. 加载并启动

以下以默认 ARM64 包为例：

```bash
docker load -i acg17-images-0.1.0-linux-arm64.tar
docker compose --env-file .env -f compose.prod.yaml config
docker compose --env-file .env -f compose.prod.yaml up -d
docker compose --env-file .env -f compose.prod.yaml ps
```

启动后访问：

```text
http://服务器IP:18000
```

查看日志：

```bash
docker compose --env-file .env -f compose.prod.yaml logs -f
```

首次创建 MySQL 数据卷时会自动初始化数据库。默认登录账号为 `admin`、密码为 `admin123`，首次登录后应立即修改密码。

## 5. 更新与数据保护

使用新 `IMAGE_TAG` 构建并上传新镜像包，服务器执行 `docker load`，同步修改 `.env` 中的 `IMAGE_TAG`，然后再次运行：

```bash
docker compose --env-file .env -f compose.prod.yaml up -d
```

需要同时备份：

- MySQL 数据卷 `acg17_mysql-data`
- 宿主机媒体目录 `/data/acg17`

停止服务且保留数据：

```bash
docker compose --env-file .env -f compose.prod.yaml down
```

生产环境不要执行 `docker compose down -v`，它会删除 MySQL 数据卷。已有数据库不会自动执行新镜像里的初始化 SQL；如果版本包含表结构变化，应先备份并单独执行升级 SQL。
