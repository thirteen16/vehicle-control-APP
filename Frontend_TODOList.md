# 🚗 CarControlAPP Android 前端开发 ToDoList（按开发顺序）

> 📌 使用说明：
> 按下面顺序逐步完成前端模块。
> 先把“能登录、能进首页、能看到车辆、能发命令、能收到实时推送”跑通，再做 PIN、历史、个人中心和美化。
>
> ✅ 当前目录结构基于：MVVM + Retrofit/OkHttp + WebSocket + DataStore

---

# 🧱 Phase 0：项目初始化（必须先做）

- [ ] 用 Android Studio 创建 Android 项目
- [ ] 确认包名、最低 SDK、语言为 Kotlin
- [ ] 配置 `build.gradle.kts`
- [ ] 添加基础依赖：
  - [ ] Retrofit
  - [ ] OkHttp
  - [ ] Gson / Moshi（二选一，建议 Gson）
  - [ ] Lifecycle ViewModel
  - [ ] LiveData / StateFlow
  - [ ] DataStore
  - [ ] RecyclerView
  - [ ] Material Components
  - [ ] WebSocket（OkHttp 自带即可）
- [ ] 配置网络权限 `INTERNET`
- [ ] 项目启动无报错

---

# 📁 Phase 1：基础目录与通用层

## common/

- [ ] `common/Constants.kt`
- [ ] `common/ResultState.kt`
- [ ] `common/Extensions.kt`

## utils/

- [ ] `utils/JwtParser.kt`
- [ ] `utils/TimeUtils.kt`
- [ ] `utils/ToastUtils.kt`

## App

- [ ] `App.kt`

> 目标：先把全局常量、通用状态、工具类补齐，后面开发更顺。

---

# 🌐 Phase 2：网络层（先打通后端）

## data/model/request/

- [ ] `LoginRequest.kt`
- [ ] `RegisterRequest.kt`
- [ ] `CommandRequest.kt`

## data/model/response/

- [ ] `ApiResponse.kt`
- [ ] `LoginResponse.kt`
- [ ] `VehicleStateResponse.kt`
- [ ] `VehicleLocationResponse.kt`
- [ ] `CommandResultResponse.kt`
- [ ] `WsMessage.kt`

## data/model/entity/

- [ ] `User.kt`
- [ ] `Vehicle.kt`
- [ ] `CommandRecord.kt`

## data/remote/api/

- [ ] `AuthApi.kt`
- [ ] `VehicleApi.kt`
- [ ] `CommandApi.kt`

## data/remote/interceptor/

- [ ] `AuthInterceptor.kt`
- [ ] `LoggingInterceptor.kt`

## di/

- [ ] `NetworkModule.kt`

> 目标：先完成 REST API 封装，确保前端能直接访问后端 `/login /vehicles /commands` 等接口。

---

# 💾 Phase 3：本地存储层（DataStore）

## data/local/

- [ ] `TokenStore.kt`
- [ ] `UserStore.kt`
- [ ] `SelectedVehicleStore.kt`
- [ ] `PinStore.kt`

## di/

- [ ] `StorageModule.kt`

> 目标：把 token、当前用户、当前选中车辆、PIN 等信息存到本地。

---

# 📦 Phase 4：Repository 层

## data/repository/

- [ ] `AuthRepository.kt`
- [ ] `VehicleRepository.kt`
- [ ] `CommandRepository.kt`
- [ ] `RealtimeRepository.kt`

## di/

- [ ] `RepositoryModule.kt`

> 目标：统一封装数据来源，UI 层不直接调用 API。

---

# 🔐 Phase 5：认证模块（先保证能登录）

## ui/auth/login/

- [ ] `LoginActivity.kt`
- [ ] `LoginViewModel.kt`
- [ ] `LoginUiState.kt`
- [ ] `res/layout/activity_login.xml`

## ui/auth/register/

- [ ] `RegisterActivity.kt`
- [ ] `RegisterViewModel.kt`
- [ ] `RegisterUiState.kt`
- [ ] `res/layout/activity_register.xml`

## ui/auth/forget/

- [ ] `ForgetPasswordActivity.kt`
- [ ] `ForgetPasswordViewModel.kt`
- [ ] `res/layout/activity_forget_password.xml`

> 目标：
> - [ ] 登录成功
> - [ ] Token 保存成功
> - [ ] 能跳转到主界面
> - [ ] 注册成功

---

# 🧭 Phase 6：主界面骨架（底部导航 / 页面容器）

## ui/main/

- [ ] `MainActivity.kt`（或改名后的主容器 Activity）
- [ ] `MainViewModel.kt`
- [ ] `MainTabState.kt`
- [ ] `res/layout/activity_main.xml`
- [ ] `res/menu/menu_bottom_nav.xml`
- [ ] `res/navigation/nav_main.xml`

> 目标：先把整个 APP 的页面容器搭出来，后续首页、控车页、我的页面都挂上去。

---

# 🏠 Phase 7：首页模块（先做展示）

## ui/home/

- [ ] `HomeFragment.kt`
- [ ] `HomeViewModel.kt`
- [ ] `HomeUiState.kt`
- [ ] `res/layout/fragment_home.xml`
- [ ] `res/layout/item_home_quick_action.xml`
- [ ] `res/layout/item_vehicle_card.xml`

> 首页建议先展示：
> - [ ] 当前车辆名称
> - [ ] 在线状态
> - [ ] 车锁状态
> - [ ] 发动机状态
> - [ ] 空调状态
> - [ ] 里程 / 油量

---

# 🚘 Phase 8：车辆模块（列表 / 详情 / 定位）

## ui/vehicle/list/

- [ ] `VehicleListFragment.kt`
- [ ] `VehicleListViewModel.kt`
- [ ] `VehicleListUiState.kt`
- [ ] `res/layout/fragment_vehicle_list.xml`
- [ ] `res/layout/item_vehicle.xml`

## ui/vehicle/detail/

- [ ] `VehicleDetailFragment.kt`
- [ ] `VehicleDetailViewModel.kt`
- [ ] `VehicleDetailUiState.kt`
- [ ] `res/layout/fragment_vehicle_detail.xml`

## ui/vehicle/location/

- [ ] `VehicleLocationFragment.kt`
- [ ] `VehicleLocationViewModel.kt`
- [ ] `res/layout/fragment_vehicle_location.xml`

> 目标：
> - [ ] 车辆列表显示成功
> - [ ] 车辆切换成功
> - [ ] 车辆详情状态成功展示
> - [ ] 定位信息能显示

---

# 🎛 Phase 9：控制模块（最核心）

## ui/control/

- [ ] `ControlFragment.kt`
- [ ] `ControlViewModel.kt`
- [ ] `ControlUiState.kt`
- [ ] `res/layout/fragment_control.xml`
- [ ] `res/layout/dialog_pin_verify.xml`（如果你希望控车前弹 PIN）

> 控制页建议优先做这些按钮：
> - [ ] 开锁 / 关锁
> - [ ] 开空调 / 关空调
> - [ ] 开窗 / 关窗
> - [ ] 发动机启动 / 熄火
> - [ ] 寻车
>
> 目标：
> - [ ] 能调用 `/commands`
> - [ ] 能拿到 `commandId`
> - [ ] 页面能显示“命令已发送 / 等待回执”

---

# 📡 Phase 10：实时通信模块（WebSocket）

## data/remote/ws/

- [ ] `WsEventListener.kt`
- [ ] `AppWebSocketClient.kt`

## data/repository/

- [ ] 完善 `RealtimeRepository.kt`

> 目标：
> - [ ] 登录后用 token 连接 WebSocket
> - [ ] 接收 `COMMAND_ACK`
> - [ ] 接收 `VEHICLE_STATE`
> - [ ] 控制页实时更新命令结果
> - [ ] 首页 / 车辆详情页实时更新车辆状态

---

# 🕘 Phase 11：命令记录模块

## ui/command/

- [ ] `CommandHistoryFragment.kt`
- [ ] `CommandResultDialog.kt`
- [ ] `CommandViewModel.kt`
- [ ] `res/layout/fragment_command_history.xml`
- [ ] `res/layout/item_command_record.xml`
- [ ] `res/layout/dialog_command_result.xml`

> 目标：
> - [ ] 能查看最近命令结果
> - [ ] 能展示 commandId、命令类型、状态、时间
> - [ ] 能弹窗显示某次命令详情

---

# 🔒 Phase 12：PIN 安全模块

## ui/pin/

- [ ] `PinSetupActivity.kt`
- [ ] `PinVerifyActivity.kt`
- [ ] `PinViewModel.kt`
- [ ] `res/layout/activity_pin_setup.xml`
- [ ] `res/layout/activity_pin_verify.xml`

> 目标：
> - [ ] 首次设置 PIN
> - [ ] 控车前校验 PIN
> - [ ] 本地保存 PIN 或 PIN 校验状态
>
> 可选增强：
> - [ ] 错误次数限制
> - [ ] 倒计时锁定

---

# 👤 Phase 13：个人中心模块

## ui/profile/

- [ ] `ProfileFragment.kt`
- [ ] `ProfileViewModel.kt`
- [ ] `res/layout/fragment_profile.xml`

> 建议先做：
> - [ ] 显示用户名 / 昵称
> - [ ] 退出登录
> - [ ] 清空本地 token
> - [ ] 跳转 PIN 设置

---

# 🎨 Phase 14：资源与 UI 美化

## res/layout/

- [ ] 补全各页面布局

## res/drawable/

- [ ] 按钮背景
- [ ] 圆角卡片背景
- [ ] 状态图标背景

## res/menu/

- [ ] 底部导航菜单

## res/navigation/

- [ ] 页面导航图

## res/values/

- [ ] `colors.xml`
- [ ] `strings.xml`
- [ ] `themes.xml`
- [ ] `dimens.xml`（可选）

## res/values-night/

- [ ] 夜间主题（可选）

> 目标：
> - [ ] 主色统一
> - [ ] 首页卡片风格统一
> - [ ] 控制按钮视觉明显
> - [ ] 状态颜色有区分

---

# 🔄 Phase 15：前后端联调

## 必测流程

- [ ] 登录成功，token 正常保存
- [ ] 自动带 token 请求后端
- [ ] 查询车辆列表成功
- [ ] 查询车辆状态成功
- [ ] 下发命令成功
- [ ] 收到 `COMMAND_ACK`
- [ ] 收到 `VEHICLE_STATE`
- [ ] 首页状态同步刷新
- [ ] 控制页结果同步刷新
- [ ] 切换车辆成功

---

# ✅ Phase 16：核心功能验收清单

## 必须全部打勾

- [ ] 登录 / 注册
- [ ] Token 本地保存
- [ ] 自动登录 / 免登录进入首页
- [ ] 车辆列表展示
- [ ] 车辆状态展示
- [ ] 远程开锁 / 关锁
- [ ] 空调开关
- [ ] 车窗控制
- [ ] WebSocket 实时推送
- [ ] 命令结果实时显示
- [ ] PIN 二次验证
- [ ] 退出登录

---

# 🚀 推荐开发顺序（最优路径）

请按下面顺序推进：

1. Phase 0：项目初始化
2. Phase 1：基础目录与通用层
3. Phase 2：网络层
4. Phase 3：本地存储层
5. Phase 4：Repository 层
6. Phase 5：认证模块
7. Phase 6：主界面骨架
8. Phase 7：首页模块
9. Phase 8：车辆模块
10. Phase 9：控制模块
11. Phase 10：实时通信模块
12. Phase 11：命令记录模块
13. Phase 12：PIN 安全模块
14. Phase 13：个人中心模块
15. Phase 14：UI 美化
16. Phase 15：前后端联调
17. Phase 16：验收

---

# 📌 第一阶段最小可运行目标

如果你想先快速做出一个“能演示”的前端，先完成这些：

- [ ] LoginActivity
- [ ] AuthApi
- [ ] TokenStore
- [ ] MainActivity
- [ ] HomeFragment
- [ ] VehicleApi
- [ ] ControlFragment
- [ ] CommandApi
- [ ] AppWebSocketClient

只要这几个跑通，你就能先实现：

- [ ] 登录
- [ ] 进入首页
- [ ] 查车辆
- [ ] 下发命令
- [ ] 收到实时推送

这就已经足够做第一版演示。
