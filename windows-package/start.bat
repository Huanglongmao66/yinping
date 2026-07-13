@echo off
chcp 65001 >nul
setlocal

set APP_HOME=%~dp0
set JAVA_HOME=%APP_HOME%runtime
set JAVA=%JAVA_HOME%\bin\java.exe

if not exist "%JAVA%" (
    echo 错误: 未找到 Java 运行时。请确保 runtime 目录包含 JDK。
    pause
    exit /b 1
)

set CP=%APP_HOME%app\yinyue-player.jar;%APP_HOME%app\lib\*

"%JAVA%" -cp "%CP%" ^
    --module-path "%APP_HOME%runtime\lib" ^
    --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.graphics,javafx.base ^
    com.yinyue.player.Main

endlocal
