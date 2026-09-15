@echo off
setlocal
cd /d "%~dp0"

if not exist "Engine.jar" (
  echo ERROR: Engine.jar is missing from this folder.
  pause
  exit /b 1
)
if not exist "UI-FX.jar" (
  echo ERROR: UI-FX.jar is missing from this folder.
  pause
  exit /b 1
)
if not exist "javafx\lib\javafx.controls.jar" (
  echo ERROR: JavaFX jars are missing. Expected folder: javafx\lib
  pause
  exit /b 1
)
if not exist "javafx\bin\glass.dll" (
  echo ERROR: JavaFX native files are missing. Expected folder: javafx\bin
  pause
  exit /b 1
)

set "PATH=%~dp0javafx\bin;%PATH%"
java --module-path "javafx\lib" --add-modules javafx.controls,javafx.fxml --enable-native-access=javafx.graphics -cp "Engine.jar;UI-FX.jar" FxMain
if errorlevel 1 pause
