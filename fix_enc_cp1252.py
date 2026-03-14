# -*- coding: utf-8 -*-
# Fix double-encoded UTF-8 via CP1252 in dashboard.html

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
        # The file was: UTF-8 bytes -> read as CP1252 -> encoded as UTF-8
        # Reverse: decode UTF-8 (done) -> encode as CP1252 -> decode as UTF-8
        cp1252_bytes = line.encode('cp1252')
        recovered = cp1252_bytes.decode('utf-8')
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

with open('fix_result2.txt', 'w', encoding='utf-8') as out:
    out.write('Fixed %d lines, kept %d lines as-is\n' % (fix_count, keep_count))
    ti = result.find('<title>')
    te = result.find('</title>')
    if ti >= 0 and te >= 0:
        out.write('Title: ' + result[ti:te+8] + '\n')
    # Show first few sidebar lines
    for i, line in enumerate(result.split('\n'), 1):
        if i >= 78 and i <= 100:
            out.write('Line %d: %s\n' % (i, line.strip()[:120]))

