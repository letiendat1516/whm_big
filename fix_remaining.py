# -*- coding: utf-8 -*-
# Fix the remaining 5 garbled Vietnamese lines in dashboard.html

filepath = r'src\main\resources\public\dashboard.html'

with open(filepath, 'r', encoding='utf-8') as f:
    lines = f.readlines()

fixed = 0
for i, line in enumerate(lines):
    # Line ~951: "Giáº£m tiá»n cá»' Ä'á»‹nh" -> "Giảm tiền cố định"
    if "Giáº£m" in line and "cá»'" in line and "FIXED" in line:
        # Find and replace the garbled option text
        lines[i] = line.replace(
            ">Giáº£m tiá»\u009cn cá»\u0091 Ä\u0091á»\u008bnh<",
            ">Giảm tiền cố định<"
        )
        if lines[i] == line:
            # Try alternate encoding
            import re
            lines[i] = re.sub(r'>Gi[^\x00-\x7F]+<', '>Giảm tiền cố định<', line)
        fixed += 1
        print(f"Fixed line {i+1}: promotion FIXED type")

    # Line ~1007: "Tráº¡ng thÃ¡i" and "Äang lÃ m" and "Nghá»‰ viá»‡c"
    elif "empStatus" in line and ("Tráº¡ng" in line or "Äang" in line):
        lines[i] = '        <div class="form-group"><label>Trạng thái</label><select id="empStatus"><option value="ACTIVE" ${data.status===\'ACTIVE\'?\'selected\':\'\'}>Đang làm</option><option value="INACTIVE" ${data.status===\'INACTIVE\'?\'selected\':\'\'}>Nghỉ việc</option></select></div>\n'
        fixed += 1
        print(f"Fixed line {i+1}: employee status")

    # Line ~1191: "ÄÃ£ chá»‰ Ä'á»‹nh kho"
    elif "chá»\u0089" in line and "kho" in line and "toast" in line:
        lines[i] = line.replace("ÄÃ£ chá»\u0089 Ä\u0091á»\u008bnh kho", "Đã chỉ định kho")
        if lines[i] == line:
            # Brute force: replace the entire toast line
            lines[i] = "    else { toast('Đã chỉ định kho','success'); hideModal(); loadTransfers(); }\n"
        fixed += 1
        print(f"Fixed line {i+1}: assigned warehouse toast")

    # Line ~1228: "Táº¡o yÃªu cáº§u Ä'iá»u chuyá»ƒn hÃ ng tá»"n"
    elif "showModal" in line and "ovWhId" in lines[i+1] if i+1 < len(lines) else False:
        lines[i] = "    showModal(`<h3>Tạo yêu cầu điều chuyển hàng tồn</h3>\n"
        fixed += 1
        print(f"Fixed line {i+1}: overstock modal title")

    # Line ~1265: "ÄÃ£ chá»‰ Ä'á»‹nh CH"
    elif "chá»\u0089" in line and "CH" in line and "toast" in line:
        lines[i] = "    else { toast('Đã chỉ định CH','success'); hideModal(); loadTransfers(); }\n"
        fixed += 1
        print(f"Fixed line {i+1}: assigned store toast")

# Also check for any remaining garbled patterns we might have missed
garbled_patterns = ['Ã¡', 'Ã©', 'Ã³', 'áº', 'á»', 'Æ°', 'Ã¢n', 'cÃ¡']
remaining = 0
for i, line in enumerate(lines):
    for pat in garbled_patterns:
        if pat in line:
            remaining += 1
            print(f"STILL GARBLED line {i+1}: {line.strip()[:100]}")
            break

with open(filepath, 'w', encoding='utf-8', newline='\n') as f:
    f.writelines(lines)

print(f"\nFixed {fixed} lines, {remaining} still garbled")

