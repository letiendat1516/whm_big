import sys
f = r'src\main\resources\public\dashboard.html'
data = open(f, 'rb').read()
if data[:3] == b'\xef\xbb\xbf':
    data = data[3:]
    print('BOM removed')
text = data.decode('utf-8')
lines = text.split('\n')
fixed = []
for line in lines:
    try:
        recovered = line.encode('raw_unicode_escape').decode('utf-8', errors='replace')
    except:
        recovered = line
    # Try latin1 approach per line
    try:
        test = line.encode('latin-1').decode('utf-8')
        fixed.append(test)
    except:
        fixed.append(line)
out = '\n'.join(fixed)
open(f, 'w', encoding='utf-8', newline='\n').write(out)
print('DONE')
verify = open(f, 'r', encoding='utf-8').readline()
print('Line 1:', verify[:80])

