# -*- coding: utf-8 -*-
import os, sys

filepath = os.path.join(r'C:\Users\utube\IdeaProjects\whm_big', 'src', 'main', 'resources', 'public', 'dashboard.html')
outlog = os.path.join(r'C:\Users\utube\IdeaProjects\whm_big', 'fix5_log.txt')

log = open(outlog, 'w', encoding='utf-8')
log.write('Starting fix...\n')
log.write('File: ' + filepath + '\n')
log.write('Exists: ' + str(os.path.exists(filepath)) + '\n')

try:
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    log.write('Total lines: ' + str(len(lines)) + '\n')

    # Line 951 (index 950)
    lines[950] = '            <option value="FIXED" ${data.promoType===\'FIXED\'?\'selected\':\'\'}>Gi\u1EA3m ti\u1EC1n c\u1ED1 \u0111\u1ECBnh</option>\n'
    log.write('Fixed line 951\n')

    # Line 1007 (index 1006)
    lines[1006] = '        <div class="form-group"><label>Tr\u1EA1ng th\u00E1i</label><select id="empStatus"><option value="ACTIVE" ${data.status===\'ACTIVE\'?\'selected\':\'\'}>\u0110ang l\u00E0m</option><option value="INACTIVE" ${data.status===\'INACTIVE\'?\'selected\':\'\'}>Ngh\u1EC9 vi\u1EC7c</option></select></div>\n'
    log.write('Fixed line 1007\n')

    # Line 1191 (index 1190)
    lines[1190] = "    else { toast('\u0110\u00E3 ch\u1EC9 \u0111\u1ECBnh kho','success'); hideModal(); loadTransfers(); }\n"
    log.write('Fixed line 1191\n')

    # Line 1228 (index 1227)
    lines[1227] = "    showModal(`<h3>T\u1EA1o y\u00EAu c\u1EA7u \u0111i\u1EC1u chuy\u1EC3n h\u00E0ng t\u1ED3n</h3>\n"
    log.write('Fixed line 1228\n')

    # Line 1265 (index 1264)
    lines[1264] = "    else { toast('\u0110\u00E3 ch\u1EC9 \u0111\u1ECBnh CH','success'); hideModal(); loadTransfers(); }\n"
    log.write('Fixed line 1265\n')

    with open(filepath, 'w', encoding='utf-8', newline='\n') as f:
        f.writelines(lines)

    log.write('File written successfully\n')

    # Verify
    with open(filepath, 'r', encoding='utf-8') as f:
        vlines = f.readlines()

    log.write('Verify line 951: ' + vlines[950].strip()[:80] + '\n')
    log.write('Verify line 1007: ' + vlines[1006].strip()[:80] + '\n')
    log.write('Verify line 1191: ' + vlines[1190].strip()[:80] + '\n')
    log.write('Verify line 1228: ' + vlines[1227].strip()[:80] + '\n')
    log.write('Verify line 1265: ' + vlines[1264].strip()[:80] + '\n')
    log.write('DONE\n')

except Exception as e:
    log.write('ERROR: ' + str(e) + '\n')
    import traceback
    log.write(traceback.format_exc() + '\n')

log.close()

