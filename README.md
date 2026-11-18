# share-kotlin

Share 客户端的 Kotlin + XML Views 重写（Android only）。

- 已完成的技术方案：见 `docs/plan.md`
- 构建：GitHub Actions（`ubuntu-latest`，JDK 21，Gradle 缓存）
- 产物：每次 push 出 debug APK artifact；打 `v*` tag 出签名 release 包

## 构建

```bash
./gradlew :app:assembleDebug
```

签名包通过 `-Pandroid.injected.signing.store.file=...` 注入，仓库内不含 keystore。

## 协议

接口基址 `https://api.weibo.cn/2/`，端点清单见 `docs/endpoints.md`。本项目不含任何官方 app key。
