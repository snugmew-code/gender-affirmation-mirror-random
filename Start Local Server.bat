@echo off
cd /d "%~dp0"
java --add-modules jdk.httpserver -jar LocalServer.jar
if errorlevel 1 (
  echo.
  echo The server could not start. Check the error above.
)
echo.
pause
