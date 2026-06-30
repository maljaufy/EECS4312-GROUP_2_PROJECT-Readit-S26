@echo off
set MAVEN_PROJECTBASEDIR=%CD%
set MAVEN_HOME=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper-home
set MAVEN_WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar
if exist "%MAVEN_WRAPPER_JAR%" (
    java -jar "%MAVEN_WRAPPER_JAR%" %*
) else (
    echo "ERROR: Couldn't find Maven wrapper JAR."
    exit /b 1
)
