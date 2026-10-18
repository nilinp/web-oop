@echo off
if exist out rmdir /s /q out
mkdir out
javac -encoding UTF-8 -cp "lib/*" -sourcepath src -d out src/library/Main.java test/library/TestRunner.java
if errorlevel 1 exit /b 1
echo Build OK
