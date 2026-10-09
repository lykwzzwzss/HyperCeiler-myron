# REDMI K90 适配

基于 wjy1065676928/HyperCeiler-myron 的 7ea5aba57，目标系统为 REDMI K90 / annibale，HyperOS 4.0.0.32 / Android 17。

- Root 检查：兼容当前 guardprovider 的 boolean 返回值及旧版 String 返回值。
- 手电筒调光：使用后置相机提供的 100 级接口，接管控制中心滑块，避免修改屏幕亮度；保留旧设备节点回退。
- 桌面抽屉：撤回作者针对桌面 6.9/7.0 的专项修复，恢复上游实现。
- 亮度上限：已核对 BrightnessRangeController 接口，保留原功能。
- 新增「禁止高温降低亮度上限」：系统框架 → 其他 → 显示与通知。默认关闭，修改后重启。仅解除已核对的软件显示温控限制，可能增加发热与耗电。

修改的手电筒、Root 返回值辅助类和温控亮度 Hook 已通过针对 Android 37、EzHookTool 1.3 / libxposed 102 的局部编译。相机选择、亮度边界、Root 返回值及显示温控 Hook 的独立回归检查通过；不代表安装后的功能实测。

完整 APK 构建尚待完成，依赖 GitHub Packages 上的 fan.miuix 1.0.13.0。未配置正式签名时项目使用测试签名，不能保证覆盖安装其他签名的版本。
