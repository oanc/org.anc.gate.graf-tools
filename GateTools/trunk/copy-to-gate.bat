@echo off
setlocal

if "%GATE_HOME%"=="" echo no-gate

set DIR=%GATE_HOME%\Plugins\ANC
copy target\ANC-GATE.jar %DIR%
copy src\main\resources\creole.xml %DIR%

:no-gate
endlocal