@echo off
setlocal

if "%GATE_HOME%"=="" echo no-gate

if "%COMPUTERNAME%"=="SCOTTY" goto scotty:

set DIR=%GATE_HOME%\Plugins\ANC
goto run

:scotty
set DIR=d:\dev\GatePlugins\ANC
goto run

:run
echo on
copy target\ANC-GATE.jar %DIR%
copy src\main\resources\creole.xml %DIR%
@echo off
goto end

:no-gate
echo GATE_HOME not set.

:end
endlocal