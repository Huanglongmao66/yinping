# 音悦播放器 Windows 安装包构建指南

## 方案一：使用 Inno Setup（推荐）

### 1. 准备 Windows 环境

在 Windows 电脑上准备以下文件：

- JDK 17（含 JavaFX）或单独下载 JavaFX Windows SDK
- Inno Setup 6（https://jrsoftware.org/isdl.php）
- 本项目的 `windows-package` 目录

### 2. 复制运行时

将 Windows 版 JDK 17 复制到 `windows-package/runtime/` 目录：

```
windows-package/
  runtime/
    bin/
      java.exe
      javaw.exe
      ...
    lib/
      javafx.base.jar
      javafx.controls.jar
      javafx.fxml.jar
      javafx.graphics.jar
      javafx.media.jar
      ...
```

推荐使用以下 JDK（已包含 JavaFX）：
- Azul ZuluFX JDK 17: https://www.azul.com/downloads/
- Liberica Full JDK 17: https://bell-sw.com/pages/downloads/

### 3. 复制应用文件

将编译好的 JAR 复制到 app 目录：

```bash
cp target/yinyue-player.jar windows-package/app/
mkdir -p windows-package/app/lib
cp lib/gson-2.10.1.jar windows-package/app/lib/
```

### 4. 准备图标（可选）

将 256x256 的 ICO 图标文件放到 `windows-package/app/icon.ico`

### 5. 编译安装包

打开 Inno Setup，加载 `windows-package/installer/setup.iss`，点击 Build。

生成的安装包位于 `windows-package/output/YinYuePlayer-Setup-1.0.0.exe`

---

## 方案二：使用 jpackage（JDK 14+）

在 Windows 命令行执行：

```cmd
jpackage ^
  --type exe ^
  --name "音悦播放器" ^
  --app-version 1.0.0 ^
  --vendor "音悦科技" ^
  --input app ^
  --main-jar yinyue-player.jar ^
  --main-class com.yinyue.player.Main ^
  --module-path "C:\javafx-sdk-17\lib" ^
  --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.graphics,javafx.base ^
  --win-dir-chooser ^
  --win-menu ^
  --win-shortcut ^
  --dest output
```

---

## 方案三：便携版 ZIP

直接打包整个 `windows-package` 目录为 ZIP，用户解压后即可运行。

```
YinYuePlayer-v1.0.0-portable.zip
  start.bat
  runtime/          (JDK + JavaFX)
  app/
    yinyue-player.jar
    lib/
      gson-2.10.1.jar
```

---

## 注意事项

1. **JavaFX 平台依赖**: Windows 版必须使用 Windows 版本的 JavaFX 模块
2. **运行时需要**: 打包时包含完整 JDK/JRE，用户无需单独安装 Java
3. **Gson 依赖**: 必须包含 `gson-2.10.1.jar` 在 classpath 中
4. **中文路径**: 所有脚本已设置 `chcp 65001` (UTF-8)，支持中文路径
