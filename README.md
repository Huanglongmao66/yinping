# 音悦音频播放器 (YinYue Audio Player)

一款基于 Java 开发的桌面端音频播放器，支持多种音频格式，提供丰富的播放控制功能和美观的用户界面。

## ✨ 功能特性

### 核心功能
- 🎵 支持多种音频格式：MP3、WAV、FLAC、OGG 等
- ▶️ 完整的播放控制：播放、暂停、停止、上一曲、下一曲
- 🔊 音量调节与静音控制
- ⏩ 进度条拖动跳转
- 📋 播放列表管理：创建、编辑、保存
- 🔀 多种播放模式：顺序播放、单曲循环、列表循环、随机播放
- 🖱️ 支持拖拽文件加载

### 进阶功能
- 🎨 现代化 UI 设计，支持亮色/暗色主题
- 📊 音频可视化：频谱图、波形图
- 📝 LRC 歌词同步显示
- 🎚️ 10段均衡器（EQ）调节
- 🎤 音频录制功能
- 🔄 音频格式转换
- 📁 本地音乐库管理
- 🔍 歌曲搜索功能
- ⌨️ 全局快捷键支持
- 🖥️ 系统托盘集成
- 📱 迷你播放模式

## 🖥️ 系统要求

| 项目 | 最低要求 | 推荐配置 |
|------|----------|----------|
| 操作系统 | Windows 10 / macOS 10.15 / Ubuntu 18.04 | Windows 11 / macOS 12+ / Ubuntu 20.04+ |
| Java 版本 | JDK 17 或更高 | JDK 17 LTS |
| 内存 | 256 MB RAM | 512 MB RAM |
| 硬盘空间 | 100 MB 可用空间 | 500 MB 可用空间 |
| 屏幕分辨率 | 800×600 | 1280×720 或更高 |

## 🚀 快速开始

### 下载安装

从 [Releases](https://github.com/your-repo/releases) 页面下载对应平台的安装包：

- **Windows**: `YinYuePlayer-Setup.exe`
- **macOS**: `YinYuePlayer.dmg`
- **Linux**: `YinYuePlayer.tar.gz`

### 从源码运行

#### 前置条件
- JDK 17+
- Maven 3.8+

#### 克隆项目
```bash
git clone https://github.com/your-repo/yinyue-audio-player.git
cd yinyue-audio-player
```

#### 编译运行
```bash
mvn clean compile
mvn javafx:run
```

#### 打包
```bash
mvn clean package
```
打包后的可执行文件位于 `target/` 目录。

## 📖 使用说明

详细使用方法请参阅：[使用说明文档](docs/使用说明.md)

### 快速上手

1. **添加音乐**
   - 拖拽音频文件到播放器窗口
   - 或点击「文件」→「打开文件」

2. **播放控制**
   - 点击播放按钮开始播放
   - 拖动进度条跳转位置
   - 使用音量滑块调节音量

3. **管理播放列表**
   - 右侧面板查看播放列表
   - 拖拽调整播放顺序
   - 右键菜单进行更多操作

### 快捷键

| 快捷键 | 功能 |
|--------|------|
| `Space` / `Ctrl+P` | 播放/暂停 |
| `Ctrl+←` / `Ctrl+→` | 上一曲/下一曲 |
| `Ctrl+↑` / `Ctrl+↓` | 音量增/减 |
| `Ctrl+M` | 静音 |
| `Ctrl+F` | 搜索 |
| `Ctrl+E` | 均衡器 |
| `F5` | 刷新音乐库 |

更多快捷键请参考 [使用说明 - 快捷键](docs/使用说明.md#十一快捷键)

## 🏗️ 项目架构

### 技术栈

- **语言**: Java 17
- **UI框架**: JavaFX 17
- **构建工具**: Maven
- **音频处理**: Java Sound API + JLayer + FLAC decoder
- **日志**: SLF4J + Logback
- **元数据**: JAudioTagger
- **测试**: JUnit 5 + AssertJ

### 模块结构

```
com.yinyue.player
├── controller/          # 控制层
│   ├── MainController      # 主控制器
│   ├── PlayerController    # 播放控制器
│   ├── PlaylistController  # 播放列表控制器
│   └── EqualizerController # 均衡器控制器
├── model/               # 数据模型
│   ├── Song                # 歌曲模型
│   ├── Playlist            # 播放列表模型
│   └── AudioInfo           # 音频信息模型
├── service/             # 业务逻辑层
│   ├── AudioPlayerService  # 音频播放服务
│   ├── PlaylistService     # 播放列表服务
│   ├── LibraryService      # 音乐库服务
│   └── AudioEffectService  # 音效处理服务
├── util/                # 工具类
│   ├── AudioUtils          # 音频工具
│   ├── FileUtils           # 文件工具
│   ├── LrcParser           # 歌词解析器
│   └── ConfigManager       # 配置管理
└── ui/                  # UI组件
    ├── MainView            # 主视图
    ├── PlayerControlBar    # 播放控制栏
    ├── PlaylistView        # 播放列表视图
    ├── VisualizerView      # 可视化视图
    └── EqualizerView       # 均衡器视图
```

架构详情请参考：[开发文档](docs/开发文档.md)

## 📂 项目文档

| 文档 | 说明 |
|------|------|
| [开发计划](docs/开发计划.md) | 开发详细计划步骤，分阶段里程碑 |
| [开发文档](docs/开发文档.md) | 需求分析、技术选型、架构设计 |
| [软件布局设计](docs/软件布局设计.md) | UI界面设计、色彩规范 |
| [使用说明](docs/使用说明.md) | 用户使用手册、常见问题 |

## 🛠️ 开发指南

### 开发环境搭建

1. **安装 JDK 17**
   ```bash
   java -version
   ```

2. **安装 Maven**
   ```bash
   mvn -version
   ```

3. **克隆项目**
   ```bash
   git clone https://github.com/your-repo/yinyue-audio-player.git
   ```

4. **导入 IDE**
   - IntelliJ IDEA: 打开项目，等待 Maven 同步完成
   - Eclipse: 导入现有 Maven 项目

### 代码规范

- 遵循阿里巴巴 Java 开发手册
- 类注释包含功能描述、作者、创建日期
- 公共方法必须有 Javadoc 注释

### 提交规范

```
feat: 新功能描述
fix: 修复bug描述
docs: 文档更新
style: 代码格式调整
refactor: 重构代码
test: 测试相关
chore: 构建/工具相关
```

### 运行测试

```bash
mvn test
```

## 🗺️ 开发路线图

### V1.0（当前版本）
- [x] 基础音频播放功能
- [x] 播放列表管理
- [x] 音乐库扫描管理
- [x] 现代化 UI 界面
- [x] 亮/暗主题切换
- [x] 音频可视化
- [x] LRC 歌词显示
- [x] 均衡器
- [x] 快捷键支持

### V1.1（计划中）
- [ ] 录音功能
- [ ] 格式转换
- [ ] 桌面歌词
- [ ] 全局快捷键
- [ ] 系统托盘优化
- [ ] 更多可视化效果

### V2.0（规划中）
- [ ] 在线音乐播放
- [ ] 歌单云同步
- [ ] 皮肤商店
- [ ] 社交分享功能

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 [MIT License](LICENSE) 许可证。

## ❓ 常见问题

**Q: 支持哪些音频格式？**
> A: 目前支持 MP3、WAV、FLAC、OGG 等常见格式。

**Q: 歌词怎么显示？**
> A: 将同名 .lrc 歌词文件和歌曲放在同一目录，播放时会自动加载。

**Q: 可以转换音频格式吗？**
> A: 可以，点击「工具」→「格式转换」即可。

更多 FAQ 请参考：[使用说明 - 常见问题](docs/使用说明.md#十二常见问题)

## 📮 联系方式

- 项目地址：[GitHub](https://github.com/your-repo/yinyue-audio-player)
- 问题反馈：[Issues](https://github.com/your-repo/yinyue-audio-player/issues)
- 邮箱：support@yinyueplayer.com

---

如果这个项目对你有帮助，欢迎点个 ⭐ Star 支持一下！
