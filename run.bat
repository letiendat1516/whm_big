@echo off
REM ============================================================
REM  WHM — Retail Store Management System
REM  Run script (Windows)
REM ============================================================
setlocal

REM Use IntelliJ's bundled JDK if JAVA_HOME not set
if "%JAVA_HOME%"=="" (
    set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\jbr"
)
set "JAVA=%JAVA_HOME%\bin\java.exe"

echo === WHM Store Management System ===
echo.

REM ── Option 1: Run pre-built fat JAR (fastest) ──────────────
if exist "target\store-management.jar" (
    echo [RUN] Launching from pre-built JAR...
    "%JAVA%" -jar "target\store-management.jar"
    goto end
)

REM ── Option 2: Compile then run ─────────────────────────────
set "JAVAC=%JAVA_HOME%\bin\javac.exe"
set "CP=%USERPROFILE%\.m2\repository\org\xerial\sqlite-jdbc\3.45.3.0\sqlite-jdbc-3.45.3.0.jar"
set "OUT=target\classes"
set "RES=src\main\resources"

if not exist "%OUT%" mkdir "%OUT%"

echo [1/3] Copying resources...
xcopy /E /Y /Q "%RES%\*" "%OUT%\" >nul

echo [2/3] Compiling...
del /f /q target\sources.txt 2>nul
for /R "src\main\java" %%f in (*.java) do echo %%f >> target\sources.txt
"%JAVAC%" -encoding UTF-8 -d "%OUT%" -cp "%CP%" @target\sources.txt
if errorlevel 1 ( echo COMPILE FAILED! & pause & exit /b 1 )

echo [3/3] Starting application...
echo.
"%JAVA%" -cp "%OUT%;%CP%" org.example.Main

:end
endlocal
pause
