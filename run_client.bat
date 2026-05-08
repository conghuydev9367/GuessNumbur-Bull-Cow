@echo off
chcp 65001 >nul
echo Compiling...
javac -encoding UTF-8 -d out src\com\guessnumber\theme\NordTheme.java src\com\guessnumber\common\Message.java src\com\guessnumber\client\ui\LoginPanel.java src\com\guessnumber\client\ui\LobbyPanel.java src\com\guessnumber\client\ui\RoomPanel.java src\com\guessnumber\client\GameClient.java
if %ERRORLEVEL% EQU 0 (
    echo Starting Client...
    java -Dfile.encoding=UTF-8 -cp out com.guessnumber.client.GameClient
) else (
    echo Compilation failed!
    pause
)
