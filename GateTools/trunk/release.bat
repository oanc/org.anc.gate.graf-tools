@echo off

rem Deploy a release version to the ANC Maven repository

setlocal
set NAME=GateTools
set RELEASE=2.0
set GROUP=-DgroupId=ANC.gate
set ARTIFACT=-DartifactId=%NAME%
set REPO=-DrepositoryId=anc-dev-release
set VERSION=-Dversion=%RELEASE%
set FILE=-Dfile=c:\dev\workspace\core\target\%NAME%-%RELEASE%.jar
set URL=-Durl=http://www.americannationalcorpus.org/maven/release

mvn deploy:deploy-file -Dpackaging=jar %GROUP% %ARTIFACT% %REPO% %VERSION% %FILE% %URL%
endlocal
