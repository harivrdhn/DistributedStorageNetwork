@echo off 
::CMD will no longer show us what command it’s executing(cleaner)

:: Starting the script
SET current=%cd%
pushd ..
pushd ..
set parent=%cd%
popd
popd


java -Xms500m -Xmx1000m -cp %parent%/lib/*;%parent%/classes poke.server.Server %1

PAUSE 
:: Lets the user read the console