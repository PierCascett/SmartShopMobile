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

:: Verifica privilegi amministratore
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo ATTENZIONE: Servono privilegi amministratore per gestire il firewall
    echo.
    echo Riavvio con privilegi amministratore...
    echo.
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

echo Privilegi amministratore OK
echo.
echo  Apertura porta 3000 nel firewall...
netsh advfirewall firewall add rule name="SmartShop Backend Temp" dir=in action=allow protocol=TCP localport=3000 >nul 2>&1
echo ✅ Porta 3000 aperta
echo.
echo Avvio server...
echo.
echo ════════════════════════════════════════
echo  Premi CTRL+C per fermare il server
echo ════════════════════════════════════════
echo.

:: Avvia il server
node server.js

:: Quando il server viene fermato (CTRL+C o chiusura finestra), chiudi la porta
echo.
echo.
echo Chiusura porta 3000 nel firewall...
netsh advfirewall firewall delete rule name="SmartShop Backend Temp" >nul 2>&1
echo Porta 3000 protetta
echo.
echo Server terminato.
echo.
pause

