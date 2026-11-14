# 计数器

原生 Android 计数工具，使用 Kotlin、Jetpack Compose 和 Room，支持离线记录与统计。

## 使用

- 单击 `+` / `−`，按计数器的设定增量立即保存，底部提示可撤销本条记录。长按 `+` 打开批量、备注和补录表单；设置中可开启每次弹窗确认。
- 首页默认使用紧凑卡片，支持大卡片和宽屏双列。卡片显示所选日期数值与截至该日累计值，点击数字查看历史。
- 卡片更多菜单支持编辑、置顶、上移和下移。新增表单的初始值、每次增量和目标放在高级设置中。
- 历史支持按计数器、分类、日期范围筛选和按天折叠，编辑、删除、补录会重新计算后续结果。
- 统计提供周期净增量、操作次数、记录天数、趋势图、计数器对比、分类汇总、正净增量占比和日历热力图。点击日历日期进入当天历史。
- 设置支持每日或累计模式、深色主题、声音、振动、提醒和音量键计数。桌面小组件为置顶优先、排序最前的计数器记录增量。
- CSV 支持原始流水与每日汇总；JSON 备份包含计数器、记录、分类和设置。导入前校验，数据库写入失败时整体回滚。
- 每次数据修改后保存本地自动快照；删除计数器、清空或导入前另存恢复快照。设置中的两个恢复入口分别恢复最近数据和最近一次这些操作前的数据。

每日模式只聚合当天增量；累计模式在初始值上累计至所选日期。统计中的“周期净增量”只统计该周期记录的变化量。首次安装从空数据开始。

## 获取 APK

在 GitHub Actions 中选择 **Build Android debug APK**，点击 **Run workflow**，选择对应分支。成功后下载 `counter-debug-apk`：

- `app-debug.apk`：安装到手机的应用。
- `app-debug-androidTest.apk`：运行设备验收测试的配套 APK。

不同 CI 运行生成的调试签名可能不同。已有数据的设备先导出 JSON；若安装提示签名不一致，重装后导入备份恢复。

原有构建工作流保持不变；`assembleDebug` 完成时会同时运行 Lint 并生成测试 APK。

## 本地构建与验收

准备 JDK 17、Android SDK 34，在 `counter-app/android` 执行：

```sh
./gradlew assembleDebug
```

安装应用和测试 APK 后运行：

```sh
adb shell am instrument -w com.example.counter.test/androidx.test.runner.AndroidJUnitRunner
```

也可使用 `scripts/verify-device.sh APK目录` 自动安装和运行，使用 `ANDROID_SERIAL` 选择设备，`ADB` 指定 adb 程序。

**设备测试会创建和清理验收数据，使用专门的测试模拟器。** 验收项目与构建记录见 [验收记录](docs/acceptance.md)。
