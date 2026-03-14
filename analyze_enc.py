# -*- coding: utf-8 -*-
# Analyze the actual bytes of the file to understand the encoding

filepath = r'src\main\resources\public\dashboard.html'

with open(filepath, 'rb') as f:
    raw = f.read()

# Find <title> tag
idx = raw.find(b'<title>')
if idx >= 0:
    chunk = raw[idx:idx+100]
    # Print hex
    with open('hex_dump.txt', 'w') as out:
        out.write('Raw bytes around <title>:\n')
        out.write(' '.join('%02x' % b for b in chunk) + '\n\n')

        # Also show what each byte is
        out.write('As chars (latin-1): ')
        out.write(chunk.decode('latin-1', errors='replace'))
        out.write('\n\n')

        out.write('As UTF-8: ')
        out.write(chunk.decode('utf-8', errors='replace'))
        out.write('\n\n')

        # Try: UTF-8 decode -> encode CP1252 -> UTF-8 decode
        try:
            step1 = chunk.decode('utf-8', errors='replace')
            step2 = step1.encode('cp1252', errors='replace')
            step3 = step2.decode('utf-8', errors='replace')
            out.write('UTF8->CP1252->UTF8: ' + step3 + '\n\n')
        except:
            out.write('CP1252 round-trip failed\n\n')

        # Try: raw as CP1252
        try:
            out.write('Raw as CP1252: ' + chunk.decode('cp1252', errors='replace') + '\n\n')
        except:
            out.write('CP1252 decode failed\n\n')

        # Try double: raw UTF-8 -> get string -> encode as raw bytes via charmap
        out.write('\nFile encoding detection:\n')
        out.write('Total file size: %d bytes\n' % len(raw))

        # Check for BOM
        if raw[:3] == b'\xef\xbb\xbf':
            out.write('BOM: UTF-8 BOM detected\n')
        elif raw[:2] == b'\xff\xfe':
            out.write('BOM: UTF-16 LE detected\n')
        elif raw[:2] == b'\xfe\xff':
            out.write('BOM: UTF-16 BE detected\n')
        else:
            out.write('BOM: none\n')

print('Done - see hex_dump.txt')

