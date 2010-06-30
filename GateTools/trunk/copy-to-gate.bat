@echo off
setlocal

if "%GATE_HOME%"=="" echo no-gate

if "%COMPUTERNAME%"=="FRANK-PC" goto FRANK-PC
if "%COMPUTERNAME%"=="SCOTTY" goto scotty
if "%COMPUTERNAME%"=="DAX" goto dax
echo Can not configure script for %COMPUTERNAME%. Please edit .bat file.
goto end

:FRANK-PC
echo Running on FRANK-PC
set DIR==%GATE_HOME%\Plugins\ANC
goto run

:dax
echo Running on Dax
set DIR=%GATE_HOME%\Plugins\ANC
goto run

:scotty
echo Running on Scotty
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