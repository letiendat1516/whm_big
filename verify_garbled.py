# -*- coding: utf-8 -*-
import os

filepath = os.path.join(r'C:\Users\utube\IdeaProjects\whm_big', 'src', 'main', 'resources', 'public', 'dashboard.html')
outlog = os.path.join(r'C:\Users\utube\IdeaProjects\whm_big', 'verify_log.txt')

with open(filepath, 'r', encoding='utf-8') as f:
    lines = f.readlines()

log = open(outlog, 'w', encoding='utf-8')

garbled_count = 0
# Check for multi-byte garbled patterns that indicate double-encoded UTF-8
patterns = ['\u00c3\u00a1', '\u00c3\u00a9', '\u00c3\u00b3', '\u00c3\u00a0', '\u00c3\u00a2',
            '\u00c4\u0090', '\u00c4\u0091', '\u00c6\u00b0',
            '\u00e1\u00ba', '\u00e1\u00bb']

for i, line in enumerate(lines, 1):
    for p in patterns:
        if p in line:
            garbled_count += 1
            log.write(f'GARBLED L{i}: {line.strip()[:120]}\n')
            break

log.write(f'\nTotal garbled lines: {garbled_count}\n')
log.write(f'Total lines: {len(lines)}\n')

# Also check for the specific garbled strings we saw before
specific = ['Giáº£m', 'Tráº¡ng thÃ¡i', 'ÄÃ£ chá»', 'Táº¡o yÃªu']
for i, line in enumerate(lines, 1):
    for s in specific:
        if s in line:
            log.write(f'SPECIFIC GARBLED L{i}: {line.strip()[:120]}\n')
            break

log.write('Verification complete.\n')
log.close()

