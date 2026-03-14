import sys
f = r'src\main\resources\public\dashboard.html'
log = open('fix_log.txt', 'w', encoding='utf-8')
try:
    data = open(f, 'rb').read()
    if data[:3] == b'\xef\xbb\xbf':
        data = data[3:]
        log.write('BOM removed\n')
    text = data.decode('utf-8')
    lines = text.split('\n')
    log.write(f'Total lines: {len(lines)}\n')
    log.write(f'Line 6 before: {lines[5][:80]}\n')

    fixed = []
    for i, line in enumerate(lines):
        try:
            test = line.encode('latin-1').decode('utf-8')
            fixed.append(test)
        except:
            fixed.append(line)

    out = '\n'.join(fixed)
    open(f, 'w', encoding='utf-8', newline='\n').write(out)

    # Verify
    verify = open(f, 'r', encoding='utf-8').readlines()
    log.write(f'Line 6 after: {verify[5][:80]}\n')
    log.write('DONE OK\n')
except Exception as e:
    log.write(f'ERROR: {e}\n')
    import traceback
    traceback.print_exc(file=log)
log.close()

