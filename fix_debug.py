import os, traceback

logf = open('fix_log2.txt', 'w', encoding='utf-8')

try:
    f = os.path.join('src', 'main', 'resources', 'public', 'dashboard.html')
    logf.write(f'File: {f}\n')
    logf.write(f'Exists: {os.path.exists(f)}\n')

    raw = open(f, 'rb').read()
    logf.write(f'Raw size: {len(raw)}\n')
    logf.write(f'First 5 bytes: {list(raw[:5])}\n')

    # Check for BOM
    if raw[:3] == b'\xef\xbb\xbf':
        raw = raw[3:]
        logf.write('BOM removed\n')
    else:
        logf.write('No BOM\n')

    logf.write(f'Byte 0-20: {list(raw[:20])}\n')

    # Find the title line bytes
    title_start = raw.find(b'<title>')
    title_end = raw.find(b'</title>')
    if title_start >= 0 and title_end >= 0:
        title_bytes = raw[title_start:title_end+8]
        logf.write(f'Title bytes: {list(title_bytes)}\n')
        logf.write(f'Title as utf8: {title_bytes.decode("utf-8", errors="replace")}\n')
        # Try decoding the content between tags as if double-encoded
        content = raw[title_start+7:title_end]
        logf.write(f'Title content bytes: {list(content)}\n')
        try:
            # Step 1: decode as utf-8 (what Python sees)
            s = content.decode('utf-8')
            logf.write(f'As UTF-8 string: {s}\n')
            # Step 2: encode back as latin-1
            b = s.encode('latin-1')
            logf.write(f'Re-encoded latin1: {list(b)}\n')
            # Step 3: decode as utf-8
            final = b.decode('utf-8')
            logf.write(f'Final result: {final}\n')
        except Exception as e2:
            logf.write(f'Round-trip failed: {e2}\n')
            # Try with cp1252 instead
            try:
                s = content.decode('utf-8')
                b = s.encode('cp1252')
                final = b.decode('utf-8')
                logf.write(f'CP1252 result: {final}\n')
            except Exception as e3:
                logf.write(f'CP1252 also failed: {e3}\n')

    logf.write('Script completed\n')
except Exception as e:
    logf.write(f'ERROR: {e}\n')
    traceback.print_exc(file=logf)

logf.close()

