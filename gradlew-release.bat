@echo off
rem Batch file to build the Lienzo UI native release executable
call "%~dp0gradlew.bat" linkReleaseExecutableNative %*
