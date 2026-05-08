@echo off
chcp 65001 >nul
echo Compiling...
javac -encoding UTF-8 -d out src\com\guessnumber\theme\NordTheme.java src\com\guessnumber\common\Message.java src\com\guessnumber\server\GameServer.java
if %ERRORLEVEL% EQU 0 (
    echo Starting Server...
    java -Dfile.encoding=UTF-8 -cp out com.guessnumber.server.GameServer
) else (
    echo Compilation failed!
    pause
)
