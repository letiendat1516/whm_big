# -*- coding: utf-8 -*-
import sys

filepath = r'src\main\resources\public\dashboard.html'

with open(filepath, 'rb') as f:
    raw = f.read()

text = raw.decode('utf-8', errors='replace')

lines = text.split('\n')
fixed_lines = []
fix_count = 0
keep_count = 0

for line in lines:
    try:
        latin1_bytes = line.encode('latin-1')
        recovered = latin1_bytes.decode('utf-8')
        if recovered != line:
            fixed_lines.append(recovered)
            fix_count += 1
        else:
            fixed_lines.append(line)
            keep_count += 1
    except (UnicodeEncodeError, UnicodeDecodeError):
        fixed_lines.append(line)
        keep_count += 1

result = '\n'.join(fixed_lines)

with open(filepath, 'w', encoding='utf-8', newline='\n') as f:
    f.write(result)

print('Fixed %d lines, kept %d lines as-is' % (fix_count, keep_count))

with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

ti = content.find('<title>')
te = content.find('</title>')
if ti >= 0 and te >= 0:
    print('Title: ' + content[ti:te+8])

