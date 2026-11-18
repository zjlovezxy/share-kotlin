# Share 客户端 Kotlin 重写方案（v1）

> 目标：把 `database633/share`（Share 3.9.6 APK 补丁项目）用 **Kotlin + XML Views + RecyclerView** 重写为一个现代 targetSdk 的 Android 客户端。
> 依据：`decoded/share_full`（97 Activity / 14,719 smali / 2 dex / targetSdk 29）作为逆向文档，不作为移植对象。

---

## 1. 边界

### 做
- 只做 Android（不需要 iOS）。
- 主干可用：时间线、详情、评论/转发/点赞、发布图文、搜索/热搜、通知、私信、我的、多账号、设置、主题。
- 现代 targetSdk、手势导航原生适配、无广告、无 hook 框架。

### 不做（写进 README，不再讨论）
直播、微博故事（VVS）、问答（QA）、音乐播放器、付费/会员/微博支付、附近的人、日历同步、悬浮窗、SSO 授权（`sinaweibo://` 唤起微博 App）、被微博 App 唤起、emoji 商店、原版主题包。
桌面小组件（Glance）列为 M4 可选项。

---

## 2. 技术栈定版

| 领域 | 选择 | 理由 |
|---|---|---|
| 语言/构建 | Kotlin 2.x（K2）、AGP 8.x、Gradle KTS、Version Catalog | K2 编译明显快于 1.9 |
| minSdk / targetSdk | 26 / 36 | 砍掉 Android 5–7 兼容分支；通知渠道、自适应图标、前台服务类型可直接用 |
| UI | XML + ViewBinding + RecyclerView（ListAdapter + DiffUtil）+ Fragment | 不用 Compose：省编译插件、省包体、省 RSS |
| 导航 | 单 Activity + androidx.navigation（fragment），深链用 navDeepLink | 97 页 → 1 张 nav graph |
| 网络 | OkHttp + kotlinx.serialization | 无 KSP/kapt；JsonElement 手动容错映射 |
| 依赖注入 | 手写 ServiceLocator（或 Koin） | 不用 Hilt，省注解处理 |
| 数据库 | Room + KSP | 与 Paging3 / Flow 集成好 |
| 分页 | Paging 3（RemoteMediator 做离线缓存） | 时间线必备 |
| 图片 | Coil 3（OkHttp 集成、磁盘缓存、GIF/WebP） | 九宫格、头像、长图统一 |
| 视频 | Media3 ExoPlayer + XML `PlayerView` | 后台播放用 `MediaSessionService` |
| 长图 | SubsamplingScaleImageView | 微博长图必须 |
| 扫码 | CameraX + ML Kit barcode（bundled） | 不依赖 GMS |
| 后台/定时 | WorkManager + `AlarmManager.setExactAndAllowWhileIdle` + ForegroundService | 定时发布要精确到点 |
| 推送 | 先 WorkManager 轮询兜底，预留厂商通道接口 | 不接 FCM（国内无 GMS） |
| 凭据 | AndroidX Security（EncryptedSharedPreferences）+ Keystore | 存 SUB/SUBP |
| 测试 | JUnit + MockWebServer 回放 fixture + Robolectric 冒烟 | 协议层是风险点，先固化 |
| 崩溃 | 自建 Logger + 可选 ACRA | 不引 Firebase |

---

## 3. 协议层（项目真正的资产）

### 3.1 已确认的事实（来自 smali）
- 基址：`https://api.weibo.cn/2/`，匿名入口 `2/guest/login`
- 公共参数：`c`（容器 id）、`s`（分页游标/页大小）、`i`、`from`（如 `10B6395010`）、`gsid`、`lang`、`luicode` / `lfid`
- 登录态：Cookie `SUB` / `SUBP`；写操作需 `X-XSRF-TOKEN`
- 已知端点（smali 常量）：`statuses/unread_friends_timeline`、`statuses/unread_hot_timeline`、`statuses/user_timeline`、`statuses/video_mixtimeline`、`statuses/modify_visible`、`container/get_item`、`comments/*`、`direct_messages/user_list`、`groupchat/{join,kick,query}`、`friendships_pages/{create,destroy}`、`poll/bind`、`vote/{vote,cancel}`、`remind/unread_count`、`captcha/get`、`account/login`、`guest/login`、`users/{relation,show_batch,update}`、`fangle/timeline`、`report/spam`、`2/map/static`
- 结构特征：`container/get_item` 卡片体系（卡片容器 id 决定渲染分支，例如热门页容器 `231619`）；`card_type` 异构

### 3.2 提取方法（顺序执行）
1. `grep -rhoE 'const-string [a-z0-9]+, "[^"]+"' smali*` → 建立 endpoint / header / 参数常量清单。
2. mitmproxy 抓真机 5 条链路：时间线、详情、评论列表、搜索、我的；记录完整 query、header、body。
3. 固化为 MockWebServer fixture（`core:network/src/test/resources/fixtures/*.json`），协议解析的每次改动都跑回放测试。
4. 风控参数（`gsid`、`X-XSRF-TOKEN`、`from`）集中在 `WeiboParams.kt` 一个文件，**改一处即可**。

### 3.3 登录
WebView 打开 `https://m.weibo.cn/login` → 用户登录 → `CookieManager.getCookie()` 取 `SUB` → 加密存 Keystore。
不做 SSO（拿不到微博 SDK 的 app key `2028810631`）。
登出/失效：检测 `-100`/`21327` 等错误码 → 引导重登；多账号各自独立 CookieJar，禁止串号。

### 3.4 请求纪律
- UA 固定为 m.weibo.cn 移动 UA；单 host 并发 ≤4，最小间隔 200ms
- 403/风控 → 指数退避 + 提示；不自动重试写操作
- 所有 DTO 容错：字段缺失/类型错/null 都不崩，日志记录一次后降级

---

## 4. 模块图

```
:app                    单 Activity、nav graph、DI 装配、深链、分享接收
:core:common            结果类型、协程工具、时间/数字格式化、日志
:core:network           OkHttp、CookieJar、公共参数、限速、fixtures
:core:model             mapi DTO（JsonElement 容错映射）
:core:data              Repository、PagingSource、Room、缓存策略
:core:auth              登录 WebView、Cookie 持久化、多账号
:core:ui                主题 token、NineGridLayout、富文本 View、底栏、通用列表项
:feature:timeline
:feature:detail
:feature:publish
:feature:search
:feature:profile
:feature:message
:feature:notification
:feature:settings
```

规则：`feature:*` 之间零依赖，只依赖 `core:*`；跨 feature 跳转通过 `:app` 的 nav graph 或 `core:ui` 里的 route 常量。

---

## 5. 渲染层（Share 里最难的部分）

| 内容 | 方案 |
|---|---|
| 正文 | `HtmlCompat.fromHtml` + `ImageGetter`（表情走 Coil + 本地缓存）+ `TagHandler`（话题/提及/@ 可点击） |
| 卡片 | `CardRenderer` 注册表：`card_type` → Renderer；未知类型降级为「链接卡片」或整卡忽略（**绝不崩**） |
| 九宫格 | 自定义 `NineGridLayout extends ViewGroup`，1/2/3/4/5–9 张布局 |
| 长文 | `isLongText` → 请求 longtext 接口；失败则 WebView 兜底 |
| 投票/文章/话题页 | 先 WebView 兜底，M4 再按需原生 |
| 视频 | ExoPlayer + `PlayerView`；缩略图用 Coil 取首帧 |

---

## 6. 里程碑与验收

### M0 骨架（3–5 天）
- 建 12 个模块 + `build-logic` convention plugins；单 Activity + nav graph；明暗主题 token；底栏（`WindowInsets` 原生处理，替代原版 70dp hack）
- 验收：clean `assembleDebug` <90s，增量 <8s，空壳 APK <5MB，冷启动 <400ms

### M1 只读（2–3 周）
- 登录（WebView + Cookie）、好友时间线（Paging3 + Room）、详情、图片查看、视频、热搜/搜索（只读）、我的主页（只读）
- 验收：断网可读 3 天缓存；低端机滚动 ≥45fps；内存峰值 <200MB；冷启动 <1.2s

### M2 交互（2–3 周）
- 评论（含楼中楼）、转发、点赞、收藏、关注/取关、删除、发图文（多图压缩）、草稿、系统分享接收
- 验收：写操作失败有明确提示与重试入口；发布中断可恢复草稿；`ACTION_SEND` 三种 mime 均可用

### M3 完整（3–4 周）
- 私信、通知中心、多账号、分组、设置、主题、定时发布（AlarmManager 精确 + WorkManager 兜底）、保存图片（MediaStore）、扫码
- 验收：多账号并发切换不串 Cookie；定时发布误差 <60s

### M4 打磨（2–3 周）
- 无障碍全量（TalkBack）、字体缩放、深色、平板、性能剖析、崩溃上报、小组件（可选）

总计：单人 10–14 周；两人并行 7–9 周。

---

## 7. 风险与缓解

| 风险 | 缓解 |
|---|---|
| 协议易变 / 被风控 | 端点常量集中一处；fixture 回放测试；远程配置（放自家 gately）下发开关 |
| Cookie 失效频繁 | 静默重登引导 + 多账号隔离 + 主动刷新 |
| 卡片渲染崩溃 | DTO 全容错 + 单卡片 try/catch + 未知卡片降级 |
| 编译变慢 | 模块化 + KSP（禁用 kapt/Hilt/Compose 插件）+ configuration-cache + build-cache |
| 合规 | 自用/小范围，不商用，不内置任何官方 key |

---

## 8. 构建性能配置（必须一次配好）

```properties
# gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.jvmargs=-Xmx3g -XX:+UseParallelGC
kotlin.incremental=true
android.nonTransitiveRClass=true
```

约定：只用 KSP；不用 kapt；不用 Hilt；不上 Compose 编译器插件；`assembleDebug` 默认跳过 lint。

---

## 9. 第一个 Sprint 的具体动作

1. 新仓库 `share-kotlin/`，`settings.gradle.kts` 登记 12 个模块 + `build-logic`
2. `:core:network` 实现 `WeiboClient`（OkHttp + CookieJar + 公共参数拦截器 + 限速拦截器）
3. mitmproxy 抓 5 条链路 → 写 fixture → `MockWebServer` 回放测试跑通
4. 打通「登录 → 时间线（纯文本卡片）→ 详情」最小闭环
5. `adb shell dumpsys gfxinfo` 记录第一版性能基线
6. 把 smali 常量清单落成 `docs/endpoints.md`（重写期间的唯一真源）

---

## 10. 待拍板

1. **包名**（发布后不可改）：候选 `com.hengye.share`（不可用）、`io.github.<你>.sharekt`
2. **minSdk 26** 是否接受（放弃 Android 5–7）
3. **私信**是否进 M3（协议面大、收益一般，可推迟到 M4）
