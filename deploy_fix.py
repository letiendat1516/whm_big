# -*- coding: utf-8 -*-
import subprocess, os

os.chdir(r'C:\Users\utube\IdeaProjects\whm_big')
log = r'C:\Users\utube\IdeaProjects\whm_big\deploy_log.txt'

with open(log, 'w', encoding='utf-8') as f:
    # Step 1: Build
    f.write("=== Step 1: Maven Build ===\n")
    r = subprocess.run(['mvn', 'package', '-DskipTests', '-q'], capture_output=True, text=True, cwd=r'C:\Users\utube\IdeaProjects\whm_big')
    f.write(f"Return code: {r.returncode}\n")
    if r.stdout: f.write(f"STDOUT: {r.stdout[-500:]}\n")
    if r.stderr: f.write(f"STDERR: {r.stderr[-500:]}\n")

    # Check jar exists
    jar = r'C:\Users\utube\IdeaProjects\whm_big\target\store-management.jar'
    f.write(f"JAR exists: {os.path.exists(jar)}\n")
    if os.path.exists(jar):
        f.write(f"JAR size: {os.path.getsize(jar)} bytes\n")

    # Step 2: Git add, commit, push
    f.write("\n=== Step 2: Git Operations ===\n")

    r = subprocess.run(['git', 'add', '-A'], capture_output=True, text=True, cwd=r'C:\Users\utube\IdeaProjects\whm_big')
    f.write(f"git add: rc={r.returncode}\n")

    r = subprocess.run(['git', 'status', '--short'], capture_output=True, text=True, cwd=r'C:\Users\utube\IdeaProjects\whm_big')
    f.write(f"git status:\n{r.stdout[:1000]}\n")

    r = subprocess.run(['git', 'commit', '-m', 'fix: correct Vietnamese encoding in dashboard.html - 5 garbled lines fixed'],
                       capture_output=True, text=True, cwd=r'C:\Users\utube\IdeaProjects\whm_big')
    f.write(f"git commit: rc={r.returncode}\n{r.stdout[:500]}\n")
    if r.stderr: f.write(f"STDERR: {r.stderr[:500]}\n")

    r = subprocess.run(['git', 'push'], capture_output=True, text=True, cwd=r'C:\Users\utube\IdeaProjects\whm_big', timeout=60)
    f.write(f"git push: rc={r.returncode}\n{r.stdout[:500]}\n")
    if r.stderr: f.write(f"STDERR: {r.stderr[:500]}\n")

    f.write("\n=== Done ===\n")

