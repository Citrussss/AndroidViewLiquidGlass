# 验证记录

- 日期：2026-11-12。
- 参考源码：本机 AndroidLiquidGlass，HEAD `eb4a0ea2e17542ea28c2e71088f7dd9acf77aad5`。
- 真机：Samsung SM-S9010，Android API 36。
- 构建：AGP 8.2.2、Gradle 8.2、SDK 34、Android Studio JBR。

## 结果

- `:demo:assembleDebug`、`:demo:assembleDebugAndroidTest`、`:glass:assembleDebug`：通过。
- `:demo:lintDebug`、`:glass:lintDebug`：通过；0 errors，合计 7 个非阻断警告，包含 Demo 的 target API、备份配置、文本国际化等。
- `CatalogInstrumentation`：全部 14 个目录目的地渲染通过，开关、滑条和标签状态检查通过。
- 额外交互检查：打开玻璃弹层、放大镜拖动通过；ScrollView 与 ListView 滚动后，固定玻璃区域的屏幕像素发生变化，背景采样更新检查通过。
- 真机截图确认：壁纸上的实时背景模糊/折射、玻璃开关、原 SDF 时钟显示正常。
- 最终 Demo APK 已安装，并启动到目录首页。

## 验证边界

尚未在 API 23–35 的设备、模拟器或其他 GPU 上运行兼容性检查。系统图片选择、双指旋转/缩放、无障碍服务和所有屏幕尺寸未做全面人工验收。Demo 为原生 View 重写，页面布局、部分手势与动画不保证逐像素或逐帧匹配原 Compose 实现。

构建产物：

- `demo/build/outputs/apk/debug/demo-debug.apk`
- `glass/build/outputs/aar/glass-debug.aar`

本地真机截图位于 `screenshots/`，构建报告位于各 module 的 `build/reports/`；这些生成文件不进入版本控制。
