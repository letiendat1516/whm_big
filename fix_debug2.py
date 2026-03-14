import os, traceback

base = r'C:\Users\utube\IdeaProjects\whm_big'
f = os.path.join(base, 'src', 'main', 'resources', 'public', 'dashboard.html')
logpath = os.path.join(base, 'fix_result.txt')

logf = open(logpath, 'w', encoding='utf-8')
try:
    logf.write('START\n')
    logf.write(f'File: {f}\n')
    logf.write(f'Exists: {os.path.exists(f)}\n')

    raw = open(f, 'rb').read()
    logf.write(f'Raw size: {len(raw)}\n')

    if raw[:3] == b'\xef\xbb\xbf':
        raw = raw[3:]
        logf.write('BOM found and removed\n')
    else:
        logf.write('No BOM\n')

    # Find title bytes
    ti = raw.find(b'<title>')
    te = raw.find(b'</title>')
    if ti >= 0:
        content = raw[ti+7:te]
        logf.write(f'Title content hex: {content.hex()}\n')
        logf.write(f'Title content bytes: {list(content)}\n')

        # Decode as UTF-8
        s = content.decode('utf-8')
        logf.write(f'Decoded UTF-8: {repr(s)}\n')

        # Check if it looks double-encoded
        # Double encoded "Quản" = C3 A1 C2 BA C2 A3 (utf-8 of "Ã¡ÂºÂ£")
        # vs correct "Quản" = 51 75 E1 BA A3 6E
        try:
            b2 = s.encode('latin-1')
            s2 = b2.decode('utf-8')
            logf.write(f'Round-trip latin1->utf8: {repr(s2)}\n')
        except Exception as e:
            logf.write(f'Latin1 round-trip failed: {e}\n')

        try:
            b2 = s.encode('cp1252')
            s2 = b2.decode('utf-8')
            logf.write(f'Round-trip cp1252->utf8: {repr(s2)}\n')
        except Exception as e:
            logf.write(f'CP1252 round-trip failed: {e}\n')

    logf.write('DONE\n')
except Exception as e:
    logf.write(f'ERROR: {e}\n')
    traceback.print_exc(file=logf)
logf.close()

