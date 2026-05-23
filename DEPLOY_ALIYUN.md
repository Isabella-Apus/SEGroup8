# 项目服务器运行说明

本文档给项目协作者使用，说明如何在阿里云服务器上更新代码、启动后端、构建前端、通过公网访问项目。

服务器信息：

- 项目目录：`/srv/SE/SEGroup8`
- 公网访问地址：`http://47.93.51.90`
- 后端端口：`8080`
- 前端开发端口：`5174`
- 正式访问端口：`80`

## 1. 登录服务器

```bash
ssh root@47.93.51.90
```

进入项目目录：

```bash
cd /srv/SE/SEGroup8
```

## 2. 拉取最新 Git 代码

先查看当前分支和本地改动：

```bash
cd /srv/SE/SEGroup8
git status
git branch
```

拉取最新代码：

```bash
git pull
```

如果服务器上有本地改动，`git pull` 可能失败。先不要强制覆盖，执行：

```bash
git status
```

看清楚是哪些文件有改动，再决定是否提交、暂存或让负责人处理。

## 3. 检查后端配置

后端配置文件位置：

```text
/srv/SE/SEGroup8/backend/src/main/resources/application.yml
/srv/SE/SEGroup8/backend/src/main/resources/application-local.yml
```

`application.yml` 默认后端端口是 `8080`，数据库是：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/segroup8_platform?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
```

服务器上的数据库账号密码应放在：

```bash
vim /srv/SE/SEGroup8/backend/src/main/resources/application-local.yml
```

示例：

```yaml
spring:
  datasource:
    username: root
    password: your_mysql_password
```

修改数据库密码后需要重新启动后端。

## 4. 初始化或更新数据库

如果数据库已经初始化过，通常不需要重复执行。

首次部署或需要重新导入初始化数据时：

```bash
cd /srv/SE/SEGroup8
mysql -uroot -p segroup8_platform < sql/init.sql
```

注意：导入 SQL 前先确认是否会覆盖已有数据。正式演示环境不要随便重置数据库。

## 5. 构建后端

```bash
cd /srv/SE/SEGroup8/backend
mvn clean package -DskipTests
```

构建成功后会生成：

```text
/srv/SE/SEGroup8/backend/target/platform-backend-0.0.1-SNAPSHOT.jar
```

## 6. 运行后端

后端长期运行推荐使用 `systemd`，它会在进程崩溃或服务器重启后自动拉起服务。`tmux` 更适合临时调试，不建议作为正式演示环境的长期方案。

### 6.1 推荐：systemd 持久在线

创建或修改服务文件：

```bash
vim /etc/systemd/system/segroup8-backend.service
```

写入：

```ini
[Unit]
Description=SEGroup8 Spring Boot Backend
After=network.target mysqld.service

[Service]
Type=simple
WorkingDirectory=/srv/SE/SEGroup8/backend
ExecStart=/usr/bin/java -jar /srv/SE/SEGroup8/backend/target/platform-backend-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=5
User=root
Environment=TZ=Asia/Shanghai

[Install]
WantedBy=multi-user.target
```

加载并启动：

```bash
systemctl daemon-reload
systemctl enable --now segroup8-backend
```

每次更新后端代码并重新打包后，重启后端：

```bash
systemctl restart segroup8-backend
```

查看状态：

```bash
systemctl status segroup8-backend
```

查看实时日志：

```bash
journalctl -u segroup8-backend -f
```

本机测试后端：

```bash
curl http://127.0.0.1:8080/swagger-ui.html
```

公网测试后端：

```text
http://47.93.51.90:8080/swagger-ui.html
```

如果 Nginx 已配置反向代理，也可以访问：

```text
http://47.93.51.90/swagger-ui.html
```

### 6.2 临时调试：tmux 运行后端

如果只是临时调试，可以用 `tmux`：

```bash
tmux new -s backend
cd /srv/SE/SEGroup8/backend
java -jar target/platform-backend-0.0.1-SNAPSHOT.jar
```

退出但保持后端运行：

```text
Ctrl + B，然后按 D
```

重新进入：

```bash
tmux attach -t backend
```

停止后端：

```text
Ctrl + C
```

## 7. 构建前端

```bash
cd /srv/SE/SEGroup8/frontend
npm install
npm run build:real
```

构建产物目录：

```text
/srv/SE/SEGroup8/frontend/dist
```

正式公网访问推荐使用 Nginx 托管 `dist`，不要长期使用 `npm run dev`。

## 8. 前端公网接口地址注意事项

当前前端代码中存在一些写死的后端地址，例如：

```text
http://localhost:8080
http://127.0.0.1:8080
```

在云服务器上，用户浏览器访问 `http://47.93.51.90` 时，前端里的 `localhost` 指的是用户自己的电脑，不是服务器。因此正式部署必须改成以下两种方案之一。

### 方案 A：推荐使用 Nginx 同域代理

前端请求使用相对路径：

```text
/api
/uploads
/ws/realtime
```

例如 `frontend/src/api/http.js` 中建议使用：

```js
baseURL: import.meta.env.VITE_API_BASE_URL || "/api"
```

图片、上传资源等地方也尽量不要写死 `localhost:8080`，而是使用空 origin 或环境变量：

```js
const API_ORIGIN = import.meta.env.VITE_API_ORIGIN || "";
```

这样公网访问统一是：

```text
http://47.93.51.90/
http://47.93.51.90/api/...
http://47.93.51.90/uploads/...
ws://47.93.51.90/ws/realtime
```

### 方案 B：临时改成公网 IP

如果暂时不改成相对路径，可以把前端里的：

```text
http://localhost:8080
http://127.0.0.1:8080
```

临时替换为：

```text
http://47.93.51.90:8080
```

这种方式需要安全组开放 `8080`，后续换服务器或域名也要重新改代码和重新构建前端，不推荐长期使用。

## 9. Nginx 配置

Nginx 用于：

- 通过 `http://47.93.51.90/` 访问前端页面
- 把 `/api/` 转发到后端 `127.0.0.1:8080`
- 把 `/uploads/` 转发到后端上传资源
- 把 `/ws/realtime` 转发到后端 WebSocket

编辑配置：

```bash
vim /etc/nginx/conf.d/segroup8.conf
```

推荐配置：

```nginx
server {
    listen 80;
    server_name 47.93.51.90;

    root /srv/SE/SEGroup8/frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:8080/uploads/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /ws/realtime {
        proxy_pass http://127.0.0.1:8080/ws/realtime;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

检查配置：

```bash
nginx -t
```

启动或重载 Nginx：

```bash
systemctl enable --now nginx
systemctl reload nginx
```

公网访问：

```text
http://47.93.51.90/
```

## 10. 临时开发模式运行前端

如果只是临时调试，可以不走 Nginx，直接跑 Vite：

```bash
cd /srv/SE/SEGroup8/frontend
npm install
npm run dev:real -- --host 0.0.0.0
```

访问：

```text
http://47.93.51.90:5174/
```

注意：

- 需要阿里云安全组开放 `5174`
- 前端仍然不能请求 `localhost:8080`
- 长期演示不要使用这种方式，推荐 `npm run build:real` 后交给 Nginx

## 11. 每次更新代码后的标准流程

后端和前端都可能变化时：

```bash
cd /srv/SE/SEGroup8
git pull

cd /srv/SE/SEGroup8/backend
mvn clean package -DskipTests
sudo systemctl restart segroup8-backend

cd /srv/SE/SEGroup8/frontend
npm install
npm run build:real

sudo nginx -t
sudo systemctl reload nginx
```

然后访问：

```text
http://47.93.51.90/
```

## 12. 常见问题

### 12.1 前端能打开，但接口请求失败

检查后端是否运行：

```bash
systemctl status segroup8-backend
curl http://127.0.0.1:8080/swagger-ui.html
```

检查前端是否还在请求 `localhost:8080`：

```bash
cd /srv/SE/SEGroup8
grep -R "localhost:8080\|127.0.0.1:8080" frontend/src
```

检查 Nginx：

```bash
nginx -t
systemctl status nginx
```

### 12.2 后端启动失败

查看日志：

```bash
journalctl -u segroup8-backend -f
```

重点看：

- 数据库账号密码是否正确
- MySQL 是否启动
- `8080` 是否被占用
- JAR 包是否已经重新构建

检查端口：

```bash
ss -lntp | grep 8080
```

### 12.3 刷新前端页面出现 404

Nginx 必须有这段配置：

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

### 12.4 修改前端后公网没变化

需要重新构建前端：

```bash
cd /srv/SE/SEGroup8/frontend
npm run build:real
systemctl reload nginx
```

浏览器可以强制刷新，或清理缓存后再访问。

### 12.5 修改后端后接口没变化

需要重新打包并重启服务：

```bash
cd /srv/SE/SEGroup8/backend
mvn clean package -DskipTests
systemctl restart segroup8-backend
```

## 13. 推荐运行方式结论

正式演示和多人协作时：

- 后端使用 `systemd`
- 前端使用 `npm run build:real` 构建后由 Nginx 托管
- 公网统一访问 `http://47.93.51.90/`
- 尽量不要开放 `8080` 和 `5174` 给公网，除非临时调试

临时调试时：

- 后端可以用 `tmux`
- 前端可以用 `npm run dev:real -- --host 0.0.0.0`
- 调试完成后切回 systemd + Nginx
