@echo off
SETLOCAL

if "%~1"=="" (
	echo Usage: %~nx0 [compile^|test^|package^|javadoc^|clean^|execute]
	exit /b 1
)

call :execute_maven %~1

:execute_maven
set COMMAND=%~1

cd %~dp0..

if /i "%COMMAND%"=="compile" (
	mvn -s config\settings.xml compile
) else if /i "%COMMAND%"=="test" (
	mvn -s config\settings.xml test
) else if /i "%COMMAND%"=="package" (
	mvn -s config\settings.xml package
) else if /i "%COMMAND%"=="javadoc" (
	mvn -s config\settings.xml javadoc:javadoc
) else if /i "%COMMAND%"=="clean" (
	mvn -s config\settings.xml clean
) else if /i "%COMMAND%"=="execute" (
	java -cp "target\nonsense-generator-1.0.jar;target\libs\*;" unipd.nonsense.App
) else (
	echo Invalid option: %COMMAND%
	echo Available options: compile, test, package, javadoc, clean, execute
	exit /b 1
)

goto :eof
