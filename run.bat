@echo off
REM 서버를 새 창에서 실행 (백그라운드 실행)
start "" jdk\bin\java -jar build/libs/KorailReserve-0.0.1-SNAPSHOT.jar

REM 서버가 켜질 시간을 확보 (필요하면 10초 이상으로 조절)
timeout /t 5 /nobreak >nul

REM Chrome으로 Swagger UI 열기
start chrome http://localhost:8080/swagger-ui/index.html