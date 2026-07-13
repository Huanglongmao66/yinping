@echo off
chcp 65001 >nul
setlocal

set APP_HOME=%~dp0
set JAVA_HOME=%APP_HOME%runtime
set JAVA=%JAVA_HOME%\bin\java.exe

if not exist "%JAVA%" (
    echo 错误: 未找到 Java 运行时。
    pause
    exit /b 1
)

set CP=%APP_HOME%app\yinyue-player.jar;%APP_HOME%app\lib\*

echo 启动音悦播放器 (调试模式)...
echo Java: %JAVA%
echo Classpath: %CP%

"%JAVA%" -cp "%CP%" ^
    --module-path "%APP_HOME%runtime\lib" ^
    --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.graphics,javafx.base ^
    -Dprism.verbose=true ^
    com.yinyue.player.Main

pause
endlocal
