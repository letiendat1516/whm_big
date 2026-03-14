import os, sys

f = os.path.join('src', 'main', 'resources', 'public', 'dashboard.html')

# Read raw bytes
raw = open(f, 'rb').read()

# Remove UTF-8 BOM if present
if raw[:3] == b'\xef\xbb\xbf':
    raw = raw[3:]

# The problem: PowerShell's Set-Content read the original UTF-8 file
# as if it were Windows-1252 (default PS encoding), then wrote it back as UTF-8.
# So each original UTF-8 byte sequence got re-encoded.
# Fix: decode as UTF-8, then encode each char back to its Latin-1 byte value,
# then decode those bytes as UTF-8.

text = raw.decode('utf-8')

# Process char by char: try to convert back through latin-1
# But some chars may have been from the BOM or already correct
# We need to handle it line by line, trying the round-trip

lines = text.split('\n')
fixed_lines = []
fail_count = 0

for i, line in enumerate(lines):
    try:
        # Try the round-trip: if the line was double-encoded,
        # encoding to latin-1 will recover the original UTF-8 bytes
        recovered = line.encode('latin-1').decode('utf-8')
        fixed_lines.append(recovered)
    except (UnicodeDecodeError, UnicodeEncodeError):
        # This line has characters outside latin-1 range (0-255)
        # which means it either:
        # 1. Was already correct UTF-8 (e.g., emoji chars like the ones in Sản phẩm table)
        # 2. Has a mix of correct and garbled text
        # Try segment-by-segment recovery
        result = []
        j = 0
        while j < len(line):
            # Try increasingly large windows
            best_end = j + 1
            for end in range(min(j + 20, len(line)), j, -1):
                seg = line[j:end]
                try:
                    recovered_seg = seg.encode('latin-1').decode('utf-8')
                    result.append(recovered_seg)
                    best_end = end
                    break
                except (UnicodeDecodeError, UnicodeEncodeError):
                    continue
            else:
                # Could not recover, keep original char
                result.append(line[j])
            j = best_end
        fixed_lines.append(''.join(result))
        fail_count += 1

output = '\n'.join(fixed_lines)

# Write back as proper UTF-8 (no BOM)
with open(f, 'wb') as out:
    out.write(output.encode('utf-8'))

# Log results
with open('fix_log.txt', 'w', encoding='utf-8') as log:
    log.write(f'Total lines: {len(lines)}\n')
    log.write(f'Lines needing segment fix: {fail_count}\n')
    log.write(f'Line 6: {fixed_lines[5][:120]}\n')
    for idx in [83, 84, 85, 86, 112, 113, 130, 140, 150]:
        if idx < len(fixed_lines):
            log.write(f'Line {idx+1}: {fixed_lines[idx][:120]}\n')
    log.write('DONE\n')

