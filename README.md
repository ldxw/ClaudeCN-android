# ClaudeCN-android

一个 Android WebView 外壳，用来接入官方项目 [host1syan/ClaudeCN](https://github.com/host1syan/ClaudeCN) 的 Web 界面：同时管理多台服务器，并在本地缓存静态资源，让页面在白屏和移动网络下更稳。

源码由官方安卓客户端逆向还原而来（见[代码来源](#代码来源)）。**本项目不含任何服务端逻辑**，只是给官方程序套的一层安卓客户端；服务端的部署、模型路由、会话管理都由官方项目提供。

纯 Java，零第三方依赖，全部界面用代码构建（无 layout XML）。`minSdk 21` / `targetSdk 34`。

---

## 代码来源

本仓库的源码从官方 ClaudeCN 安卓客户端的 APK 逆向还原得到，官方并未公开这一端的源码。还原过程是把多 dex（`classes.dex` ~ `classes5.dex`）合并回单一工程，smali 逐类还原为 Java，资源与清单按原包还原，再重建成标准 Gradle 工程。

还原口径：

- 原包 md5 `1218a9414d66f228a6f045e96e283b80`，包名 `com.host1syan.claudecn`，版本 `1.0.1`（versionCode 3）。
- 保持纯 Java，不含 Kotlin / Compose；无 layout XML，界面全部由 Java 代码构造。
- 零第三方依赖，`app/build.gradle` 中没有 `implementation` 行，与 `android.useAndroidX=false` 一致。
- 权限、`minSdk 21` / `targetSdk 34`、启动 Activity 与原包一致。
- 顶栏的 `⟳` 刷新按钮是在还原基础上新增的功能，原包没有。
- 使用自建证书签名，与原包签名不同，**不能覆盖安装原版 APK**，需先卸载。

---

## 官方服务端

| 项 | 说明 |
| --- | --- |
| 仓库 | <https://github.com/host1syan/ClaudeCN> |
| Docker 镜像 | `host1syan/claude-cn` |
| 默认端口 | `3456` |
| H5 访问令牌 | 环境变量 `CLAUDE_H5_TOKEN` |
| WebDAV | 环境变量 `WEBDAV_USER` / `WEBDAV_PASSWORD` |

官方程序是一套全中文提示词的仿 Claude 智能体工作台，支持 macOS、Windows、Linux，提供终端 CLI、本地服务端和桌面客户端三种入口；容器化部署后由浏览器打开图形界面，并支持 H5 远程访问。

以 Docker 部署为例：

```bash
docker pull host1syan/claude-cn

docker run -d --name claude-cn \
  -p 3456:3456 \
  -v claude-data:/data \
  -e CLAUDE_H5_TOKEN=<token> \
  -e WEBDAV_USER=admin \
  -e WEBDAV_PASSWORD=<password> \
  host1syan/claude-cn
```

启动后浏览器访问 `http://<主机>:3456`。若设置了 `CLAUDE_H5_TOKEN`，在本 App 里添加该地址后，首次打开网页时输入一次令牌；之后注入脚本会自动给 `fetch` / `XMLHttpRequest` / `WebSocket` 带上凭证，不必重复输入。

也可以在 Releases 页下载 macOS / Windows / Linux 的桌面安装包，不经过 Docker。部署细节见官方仓库的 `DOCKER_README.md` 与 `docker/USAGE.md`。

> 服务端升级后若改动前端 DOM 结构，可能影响状态圆点的兜底匹配（见「已知限制」）；WebSocket 解析为主要来源，通常不受影响。

---

## 功能

### 多服务器标签

- 每台服务器一条配置，底部标签横向滚动切换；标签过多时可左右滑动。
- 顶栏右侧三枚动作按钮固定不动：`⟳` 刷新、`＋` 添加服务器、`⚙` 设置。
- 点击标签切换页面，长按标签进入该服务器的编辑页。
- 同时最多保活 6 个 WebView，超出后按顺序销毁最早的，避免内存堆积。

### 服务器配置

- 名称（仅用于区分，可留空）与地址两项。
- 地址省略协议时自动补 `https://`，末尾 `/` 自动去掉。
- 编辑页可删除服务器，删除时一并清掉该服务器的本地缓存。

### 令牌注入

服务器若开启了 H5 访问令牌，注入脚本会自动给页面发出的请求带上凭证，无需在网页里重复输入：

- `fetch`：在 `headers` 上补 `Authorization: Bearer <token>`（兼容 `Headers` 实例、数组、普通对象三种形态，已有同名头则不覆盖）。
- `XMLHttpRequest`：包装 `open` / `setRequestHeader` / `send`，脚本未显式设置时补同样的头。
- `WebSocket`：在 URL 上追加 `token=<token>`（URL 里已有 `token=` 则不加）。

注入点：入口 HTML 的 `<head>` 之后插入一段 `<script>`，并带上 `<!--claudecn-inject-->` 标记避免重复注入。入口 HTML 本身也走本地缓存，因此注入在每次加载时都会生效。

### 运行状态指示

标签上的小圆点反映该服务器的实时状态，由注入脚本推算：

- 🟢 **运行中**：会话轮次进行中，或还有后台任务未结束。
- 🟡 **等待授权**：有尚未处理的权限请求。
- 无圆点：空闲。

状态来源有两条，互为补充：

1. 解析 WebSocket 消息（`session_state` / `status` / `permission_request*` / `permission_requests_snapshot` / `permission_resolved` / `message_complete` / 各类 `content_*` / `system_notification` 的 `task_started`、`task_progress`、`task_notification`、`session_cleared`）。
2. `MutationObserver` 兜底扫描 DOM，匹配中、英、日、韩四种语言的 `aria-label` 与 `data-testid`，限流 500 ms。

结果通过 `window.ClaudecnHost.status(run, attn)` 回传给原生 `StatusBridge`，在主线程更新圆点。另导出 `window.__cnForceReconnect()` 供调试时强制断开所有 WebSocket。

### 静态资源缓存

按服务器分目录存放，路径 `files/assets/<serverId>/`，文件名取 `SHA-1(url)`。

**缓存判定**（`CachePolicy`）：

| 情况 | 判定 |
| --- | --- |
| 路径含 `/api/` | `NEEDS_AUTH`，不缓存 |
| 路径含 `/ws/`、`/proxy/`、`/health`、`/webdav/` | `PASS`，交给 WebView |
| 以 `.php` / `.asp` / `.jsp` / `.cgi` 结尾 | `PASS` |
| 以 `/`、`.html`、`.htm` 结尾 | `PASS` |
| `.js` `.mjs` `.css` `.woff` `.woff2` `.ttf` `.otf` `.eot` `.png` `.jpg` `.jpeg` `.gif` `.webp` `.svg` `.ico` `.bmp` `.wasm` `.map` `.json` | `CACHEABLE_STATIC` |
| 其他 | `PASS` |

**行为细节**：

- 只拦截 GET；非 GET 一律放行。
- 命中磁盘直接返回，并附 `Cache-Control: no-store` 防止 WebView 再叠一层缓存。
- 未命中时异步补抓（线程池 3 条），返回 `null` 让 WebView 自己取，用户不感知等待。
- 单条上限 8 MiB；先写 `<name>.part`，完整写完才 `renameTo` 正式名，避免半截文件。
- 入口 HTML 单独处理：命中缓存先立即返回，同时异步重新拉取比对内容，有差异再替换；未命中则同步下载后落盘（4 MiB 上限，连接 10 s / 读取 20 s 超时）。
- 抓取带 `Accept-Encoding: identity` 与已配置的 Bearer 令牌。

WebView 侧开启 `LOAD_CACHE_ELSE_NETWORK`、DOM Storage、Database，允许第三方 Cookie；关闭 `allowFileAccess` / `allowContentAccess`。

### 刷新

- **单击 `⟳`**：软刷新。使该服务器的入口 HTML 失效，重置状态圆点，`reload()` 当前页面。
- **长按 `⟳`**：硬刷新。在软刷新的基础上清空该服务器的全部本地缓存，并提示「已清空该服务器缓存，正在重新加载」。

### 缓存管理

设置 → 缓存管理：

- 逐台列出已占空间（人类可读单位）与文件数（不含 `.part`）。
- 单台可「清空这台的缓存」。
- 底部显示「合计 X」，可「清空全部」，带二次确认对话框。

### 外观

- 主题三选一：跟随系统 / 浅色 / 深色。修改后回到主界面自动 `recreate()` 生效。
- 全部颜色集中在 `Palette`，浅深两套；界面元素由 `Ui` 工具类统一构造（卡片、圆角按钮、图标按钮等）。

---

## 环境要求

| 项目 | 版本 |
| --- | --- |
| JDK | 17 |
| Gradle | 8.7（随 wrapper 提供，无需预装） |
| Android Gradle Plugin | 8.5.2 |
| compileSdk / targetSdk | 34 |
| minSdk | 21 |
| Android SDK Build-Tools | 34.0.0 |
| AndroidX | 未使用（`android.useAndroidX=false`） |

没有任何 `implementation` 依赖；`settings.gradle` 里的 google / mavenCentral 仓库仅用于拉取 AGP 与 Gradle 插件。

---

## 本地构建

```bash
git clone https://github.com/<your-name>/ClaudeCN-android.git
cd ClaudeCN-android

# 指定本机 SDK（未设置 ANDROID_HOME / ANDROID_SDK_ROOT 时）
echo "sdk.dir=/path/to/Android/sdk" > local.properties

./gradlew assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

其他常用命令：

```bash
./gradlew lintDebug            # 静态检查，报告在 app/build/reports/
./gradlew clean                # 清理
./gradlew assembleRelease      # release 变体（默认 unsigned）
```

`local.properties` 已在 `.gitignore` 中；CI 通过 `ANDROID_HOME` 定位 SDK，不依赖该文件。

---

## CI 构建

`.github/workflows/android.yml` 在每次 push 到 `main` / `master`、每次 pull request，以及手动 `workflow_dispatch` 时执行：

1. `actions/checkout@v4`
2. `actions/setup-java@v4`（temurin 17）
3. `android-actions/setup-android@v3`，安装 `platforms;android-34` 与 `build-tools;34.0.0`
4. `gradle/actions/setup-gradle@v4`（含依赖缓存）
5. `./gradlew assembleDebug`
6. `./gradlew lintDebug`
7. 上传 `app/build/outputs/apk/debug/*.apk` 为 artifact `claudecn-debug`；lint 报告单独作为 `lint-report` 上传（失败时也传）

流水线只产出 debug APK，不涉及签名密钥。要发正式包请走下面的签名流程，或在仓库 Secrets 里配置 keystore 后自行扩展 workflow。

---

## 签名与安装

release 变体默认 **unsigned**（`app/build.gradle` 中没有 `signingConfig`），需要手动签名：

```bash
./gradlew assembleRelease

BT=$ANDROID_HOME/build-tools/34.0.0

$BT/zipalign -p -f 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  app-release-aligned.apk

$BT/apksigner sign \
  --ks my-release.jks --ks-key-alias mykey \
  --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
  --out app-release-signed.apk app-release-aligned.apk

$BT/apksigner verify --print-certs app-release-signed.apk
```

生成 keystore：

```bash
keytool -genkeypair -v -keystore my-release.jks -alias mykey \
  -keyalg RSA -keysize 2048 -validity 10000
```

**签名一致性很重要**：Android 只允许同一包名、同一签名证书的 APK 覆盖升级。

- 本项目自签的 release 包 **不能** 覆盖安装从别处获得的官方签名的包，需先卸载。
- 同一签名证书的连续版本可以正常覆盖升级，用户数据（服务器列表、本地缓存）保留。

安装：

```bash
adb install -r app-release-signed.apk
```

首次打开为空状态页，点「添加服务器」填入地址即可。

---

## 目录结构

```
ClaudeCN-android/
├── .github/workflows/android.yml     # CI：构建 debug + lint + 上传产物
├── app/
│   ├── build.gradle                  # 应用模块：版本、SDK、Java 17
│   ├── proguard-rules.pro            # 默认规则（release 未开启混淆）
│   └── src/main/
│       ├── AndroidManifest.xml       # 权限与四个 Activity
│       ├── java/com/host1syan/claudecn/
│       │   ├── cache/
│       │   │   ├── AssetCache.java       # 磁盘缓存读写、大小统计、清空
│       │   │   ├── CacheKey.java         # URL → SHA-1 文件名映射
│       │   │   ├── CachePolicy.java      # 缓存判定与 MIME 映射
│       │   │   ├── CnWebViewClient.java  # 请求拦截、入口 HTML 处理
│       │   │   └── Injector.java         # 入口 HTML 的 JS 注入
│       │   ├── model/ServerConfig.java   # 服务器配置与 URL 规范化
│       │   ├── store/ServerStore.java    # SharedPreferences 持久化
│       │   └── ui/
│       │       ├── MainActivity.java     # 标签、WebView 管理、刷新
│       │       ├── ServerEditActivity.java
│       │       ├── CacheActivity.java
│       │       ├── SettingsActivity.java
│       │       ├── StatusBridge.java     # JS → 原生状态回传
│       │       ├── StatusDot.java        # 状态圆点自绘
│       │       ├── TabView.java          # 单个标签自绘
│       │       ├── Palette.java          # 浅/深配色表
│       │       ├── Theme.java            # 主题模式存取与应用
│       │       └── Ui.java               # 界面构造工具
│       └── res/
│           ├── drawable/                 # 应用图标矢量图
│           ├── mipmap-*/                 # 各密度启动图标
│           └── values/                   # strings / colors / styles
├── build.gradle                      # 根构建脚本（仅声明 AGP）
├── settings.gradle                   # 仓库与模块声明
├── gradle.properties                 # Gradle 与 Android 构建开关
├── gradle/wrapper/                   # Gradle 8.7 wrapper
├── gradlew / gradlew.bat
├── .gitignore
└── README.md
```

### 数据存放位置

| 内容 | 位置 |
| --- | --- |
| 服务器列表 | `SharedPreferences` 文件 `claudecn_servers`（键 `order`、`url_<id>`、`name_<id>`、`token_<id>`） |
| 静态资源缓存 | `files/assets/<serverId>/` |
| 主题模式 | 由 `Theme` 写入默认 `SharedPreferences` |
| Cookie / 网页本地存储 | 系统 WebView 数据目录 |

卸载应用会一并清除以上全部数据。

---

## 已知限制

- `usesCleartextTraffic="true"`：允许 http 地址，便于在内网部署官方服务端。公网使用时建议只填 https。
- release 变体不带签名配置，必须自行签名后才能安装。
- 缓存只覆盖静态资源与入口 HTML，`/api/` 等动态请求始终走网络。
- 令牌以明文保存在 `SharedPreferences` 中，依赖 Android 的应用沙箱隔离；设备已 root 时无法防止读取。
- 状态圆点的 DOM 兜底依赖页面上的 `aria-label` / `data-testid`，前端结构大改后可能需要更新匹配串；WebSocket 解析为主要来源。
- 同时保活 6 个 WebView，切换超出的标签会重新加载页面。
- 未做横竖屏分栏、平板双栏布局。

---

## 免责声明

本项目是一个 WebView 外壳，仅负责展示用户自己填写的网页地址并在本地缓存其静态资源，本身不含服务端能力。Android 端代码由官方客户端逆向还原而来，服务端功能由官方项目 [host1syan/ClaudeCN](https://github.com/host1syan/ClaudeCN) 提供，本项目与官方均无隶属关系。使用者需自行确保对所连接的服务器与内容拥有相应权限，并遵守相关服务的使用条款。
