@echo off
rem Batch file to build the Lienzo UI native debug executable
call "%~dp0gradlew.bat" linkDebugExecutableNative %*
