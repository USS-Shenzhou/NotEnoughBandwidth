# Not Enough Bandwidth (NEB) - 1.19.2 Forge Port Plan

## 1. 构建环境切换 (NeoForge 1.21 -> Forge 1.19.2)
- **目标**: 将项目从 `net.neoforged.moddev` 迁移到 ForgeGradle 5/6。
- **任务**:
  - 重写 `build.gradle` 和 `gradle.properties`。
  - 删除 `src/main/templates` 等特定于 NeoForge 模板系统的内容。
  - 将 `src/main/resources/META-INF/neoforge.mods.toml` 替换为 `src/main/resources/META-INF/mods.toml`。

## 2. 核心架构调整与裁切
由于 1.19.2 和 1.21 的网络协议存在巨大差异，本次移植将优先保证核心功能：
- **移除 `CustomPacketPayload` 紧凑包头优化**: 1.19.2 没有 `Configuration Phase`，且 Forge 有自己的一套网络注册机制。为了保证兼容性，这一部分功能将被暂时移除或完全重写。
- **保留并移植 Netty 聚合与压缩 (核心)**: 这是省流量的大头。移植 `AggregationManager` 和 `ZstdHelper`，调整 `ConnectionMixin` 以适配 1.19.2 的 `Connection` 类签名。
- **延迟区块缓存 (DCC)**: 尝试移植。需要寻找 1.19.2 中对应的 `ChunkMap` 和区块追踪/卸载逻辑的 Mixin 注入点。

## 3. 代码级适配与重构
- **包名替换**: 将所有的 `net.neoforged.*` 替换为 `net.minecraftforge.*`。
- **配置系统**: 适配 Forge 1.19.2 的配置加载方式（或者保留原有的基于 Gson 的自定义配置文件）。
- **事件注册**: 将主类 `NotEnoughBandwidth.java` 中的事件总线注册（如 `FMLCommonSetupEvent`, `RegisterPayloadHandlersEvent`）替换为 1.19.2 对应的方式。
- **Mixin 修复**: 逐一修复 `mixin` 包下所有类的报错。由于 MCP/Mojmap 映射的不同，很多方法名和描述符都会改变。

## 4. 依赖管理
- **Zstd-JNI**: 确保在 1.19.2 的 Forge 环境中能正确引入并打包 `com.github.luben:zstd-jni`（Jar-in-Jar 或 ShadowJar）。

## 移植步骤
1. [x] 创建并切换到新分支 `port-1.19.2-forge`
2. [ ] 初始化 1.19.2 Forge MDK 构建环境
3. [ ] 删减 1.21 特定代码并解决基础编译错误
4. [ ] 移植核心 Netty 拦截与 Zstd 压缩逻辑
5. [ ] 修复并测试 Mixin 注入
6. [ ] 测试打包并在本地服务端验证流量压缩效果