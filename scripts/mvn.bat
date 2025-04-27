@echo off
SETLOCAL

if "%~1"==""
(
	echo Usage: %~nx0 [compile^|test^|package^|javadoc^|clean]
	exit /b 1
)

call :execute_maven %~1
exit /b %ERRORLEVEL%

:execute_maven
setlocal
	set COMMAND=%~1
	cd %~dp0..

	if "%COMMAND%"=="compile"
	(
		mvn -s settings.xml compile
	)
	else if "%COMMAND%"=="test"
	(
		mvn -s settings.xml test
	)
	else if "%COMMAND%"=="package"
	(
		mvn -s settings.xml package
	)
	else if "%COMMAND%"=="javadoc"
	(
		mvn -s settings.xml javadoc:javadoc
	)
	else if "%COMMAND%"=="clean"
	(
		mvn -s settings.xml clean
	)
	else
	(
		echo Invalid option: %COMMAND%
		echo Available options: compile, test, package, javadoc, clean
		exit /b 1
	)
endlocal
goto :eof
