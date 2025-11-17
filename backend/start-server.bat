@echo off
title SmartShop Backend Server
color 0A
cd /d "%~dp0"

echo.
echo ========================================
echo   SmartShop Backend Server
echo   http://localhost:3000
echo ========================================
echo.

:: Se il primo argomento è "elevated" significa che siamo stati già rilanciati con privilegi
if "%~1"=="elevated" (
    set IS_ELEVATED=1
) else (
    set IS_ELEVATED=0
)

:: Verifica privilegi amministratore solo se non siamo già in modalità elevated
if "%IS_ELEVATED%"=="0" (
    net session >nul 2>&1
    if %errorLevel% neq 0 (
        echo ATTENZIONE: Servono privilegi amministratore per gestire il firewall (se necessario)
        echo.
        echo Riavvio con privilegi amministratore...
        echo.
        powershell -Command "Start-Process -FilePath '%~f0' -ArgumentList 'elevated' -Verb RunAs"
        exit /b
    )
)

echo Privilegi amministratore OK
echo.
echo Avvio server...
echo.
echo ════════════════════════════════════════
echo  Premi CTRL+C per fermare il server
echo ════════════════════════════════════════
echo.

:: Avvia il server (server.js gestisce ora apertura/rimozione firewall e graceful shutdown)
node server.js %*

:: Al termine, mostra messaggi finali
echo.
echo Server terminato.
echo.
pause
