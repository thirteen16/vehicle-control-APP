# vehicle-control-APP 前端说明（实际代码版）

基于 **Android + Kotlin + MVVM + Retrofit + OkHttp + WebSocket + DataStore** 的远程车辆控制前端 APP。

这份 README 按你当前“最终集成版前端代码”整理，适合用于：

- 前端项目说明
- 与后端联调
- 毕设答辩展示
- 论文中“客户端设计”“界面设计”“系统实现”章节

## 1. 项目定位

本项目是远程车辆控制系统的 Android 客户端。主要面向普通用户，提供：

- 账号登录 / 注册 / 找回密码
- 主页查看车辆信息
- 远程控制车辆
- 地图查看车辆定位
- 查看操作历史
- 管理昵称、手机号、PIN、安全与服务器配置

当前 UI 已统一为深色科技风，主页与车辆信息已合并为一个主页面。

## 2. 当前实际功能

### 2.1 账号与安全
- 登录
- 注册
- 找回密码
- 记住密码
- 自动登录
- PIN 设置
- PIN 验证
- 修改昵称
- 换绑手机号
- 重置密码

### 2.2 车辆与控制
- 主页展示车辆总数
- 当前绑定车辆详细信息展示
- 刷新当前车辆状态
- 切换当前车辆
- 地图定位入口
- 上锁 / 解锁
- 启动 / 关闭发动机
- 开启 / 关闭空调
- 打开 / 关闭车窗
- 同步车辆状态
- 最近一次操作结果展示

### 2.3 历史与资料
- 操作历史列表
- 按车辆范围筛选
- 按状态筛选（全部 / 成功 / 失败 / 进行中）
- 个人信息页
- 服务器地址配置

### 2.4 实时能力
- WebSocket 全局连接
- 实时接收命令 ACK
- 实时接收车辆状态
- 主页与控制页联动刷新

## 3. 技术栈
- Kotlin
- Android SDK
- MVVM
- LiveData / ViewModel
- Retrofit
- OkHttp
- WebSocket
- DataStore / SharedPreferences（依本地实现）
- OSMdroid 地图定位

## 4. 前端实际项目结构

```text
app/src/main
├── AndroidManifest.xml
├── java/com/example/app
│   ├── App.kt
│   ├── MainActivity.kt
│   ├── common
│   ├── data
│   │   ├── local
│   │   ├── model
│   │   │   ├── entity
│   │   │   ├── request
│   │   │   └── response
│   │   ├── remote
│   │   │   ├── api
│   │   │   ├── interceptor
│   │   │   └── ws
│   │   └── repository
│   ├── di
│   ├── ui
│   │   ├── auth
│   │   ├── command
│   │   ├── control
│   │   ├── home
│   │   ├── main
│   │   ├── pin
│   │   ├── profile
│   │   ├── settings
│   │   └── vehicle
│   └── utils
└── res
    ├── color
    ├── drawable
    ├── layout
    ├── menu
    ├── mipmap-*
    ├── values
    ├── values-night
    └── xml
```

## 5. 页面结构

### 登录相关
- `LoginActivity`
- `RegisterActivity`
- `ForgetPasswordActivity`

### 主框架
- `MainActivity`
- 底部导航 4 个 Tab：
  - 主页
  - 控制
  - 历史
  - 我的

### 主页
- `HomeFragment`
- 展示车辆总数
- 展示当前绑定车辆
- 展示其他车辆列表
- 提供刷新和定位入口

### 控制页
- `ControlFragment`
- 展示当前车辆状态
- 提供远程控制按钮
- 展示最近一次操作结果

### 历史页
- `CommandHistoryFragment`
- 展示命令记录
- 支持按车辆范围与结果筛选
- 点击单条记录查看详情弹窗

### 我的页
- `ProfileFragment`
- 展示昵称、用户名、手机号、当前车辆、PIN 状态
- 提供修改昵称、换绑手机号、重置密码、PIN 管理、服务器配置、退出登录

### 车辆扩展页
- `VehicleDetailActivity`
- `VehicleLocationActivity`

## 6. 数据层设计

### 6.1 本地存储（local）
用于保存本地状态，例如：
- token
- 最近登录账号
- 记住密码 / 自动登录开关
- 当前选中车辆
- PIN
- 服务器地址配置

### 6.2 请求模型（request）
用于封装前端请求体，例如：
- 登录
- 注册
- 重置密码
- 下发命令
- 修改昵称
- 发送换绑手机号验证码
- 换绑手机号

### 6.3 响应模型（response）
用于封装后端响应数据，例如：
- 登录返回
- 当前用户信息
- 车辆状态
- 车辆定位
- 命令提交结果
- 命令历史
- WebSocket ACK / 状态消息

### 6.4 Repository
Repository 层用于统一管理数据来源，解耦 ViewModel 与网络实现。

## 7. 网络与实时通信

### 7.1 HTTP
使用 Retrofit + OkHttp 调用后端接口：
- AuthApi
- VehicleApi
- CommandApi
- UserProfileApi

### 7.2 JWT
登录成功后保存 token，请求头统一添加：

```http
Authorization: Bearer <token>
```

### 7.3 WebSocket
App 全局维持一条 WebSocket 连接，用于接收：
- `COMMAND_ACK`
- `VEHICLE_STATE`

这部分数据由 `AppRealtimeViewModel` 统一分发到主页和控制页。

## 8. 与后端接口对应关系

### 8.1 认证
- `POST /register`
- `POST /login`
- `GET /me`
- `POST /auth/send-reset-code`
- `POST /auth/reset-password`

### 8.2 用户资料
- `POST /user/update-nickname`
- `POST /user/send-change-phone-code`
- `POST /user/change-phone`

### 8.3 车辆
- `GET /vehicles`
- `GET /vehicles/{vehicleId}/state`
- `GET /vehicles/{vehicleId}/location`

### 8.4 命令
- `POST /commands`
- `GET /commands/{commandId}`

### 8.5 WebSocket
- `ws://<host>:<port>/ws/app?token=<JWT>`

## 9. 界面设计原则

当前界面风格采用：
- 深色科技风背景
- 渐变卡片
- 蓝色高亮按钮
- 清晰的标题与层级
- 减少调试信息暴露
- 更贴近普通用户视角

## 10. 启动与构建

### 10.1 环境建议
- Android Studio
- JDK 17 或与 Gradle 匹配版本
- Android SDK
- Gradle Wrapper

### 10.2 启动步骤
1. 打开项目
2. 等待 Gradle 同步
3. 配置模拟器或真机
4. 启动后端服务
5. 在“我的 → 服务器配置”中填写后端地址
6. 运行 APP

### 10.3 构建命令
```bash
./gradlew assembleDebug
```

Windows：
```bash
gradlew.bat assembleDebug
```

## 11. 典型联调流程

1. 后端启动  
2. 使用测试账号登录  
3. 主页加载车辆列表  
4. 控制页发起命令  
5. 后端通过 MQTT 转发  
6. 模拟车机返回 ACK 与状态  
7. WebSocket 推送到前端  
8. 主页 / 控制页 / 历史页实时更新  

## 12. 适合论文写作的前端亮点
1. MVVM 架构清晰  
2. 前后端解耦  
3. WebSocket 实时交互  
4. 多状态处理较完整  
5. 服务器地址可配置  
6. 界面风格统一、交互更贴近产品化  

## 13. 后续可扩展方向
- RecyclerView + DiffUtil 优化主页列表刷新
- 地图轨迹回放
- 命令历史分页
- 车辆告警中心
- 推送通知与消息中心

## 14. License
仅用于学习、课程设计、毕业设计与个人项目展示。
