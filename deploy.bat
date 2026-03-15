@echo off
cd /d C:\Users\utube\IdeaProjects\whm_big
echo === Building === > deploy_log.txt
call mvn package -DskipTests -q >> deploy_log.txt 2>&1
echo Build RC: %ERRORLEVEL% >> deploy_log.txt
echo === Git Add === >> deploy_log.txt
git add -A >> deploy_log.txt 2>&1
echo === Git Commit === >> deploy_log.txt
git commit -m "fix: correct Vietnamese encoding in dashboard.html" >> deploy_log.txt 2>&1
echo === Git Push === >> deploy_log.txt
git push >> deploy_log.txt 2>&1
echo === Done === >> deploy_log.txt

