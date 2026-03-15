# -*- coding: utf-8 -*-
"""Fix the 5 remaining garbled Vietnamese lines in dashboard.html"""

filepath = r'src\main\resources\public\dashboard.html'

with open(filepath, 'r', encoding='utf-8') as f:
    lines = f.readlines()

fixes = {
    950: '            <option value="FIXED" ${data.promoType===\'FIXED\'?\'selected\':\'\'}>Giảm tiền cố định</option>\n',
    1006: '        <div class="form-group"><label>Trạng thái</label><select id="empStatus"><option value="ACTIVE" ${data.status===\'ACTIVE\'?\'selected\':\'\'}>Đang làm</option><option value="INACTIVE" ${data.status===\'INACTIVE\'?\'selected\':\'\'}>Nghỉ việc</option></select></div>\n',
    1190: "    else { toast('Đã chỉ định kho','success'); hideModal(); loadTransfers(); }\n",
    1227: "    showModal(`<h3>Tạo yêu cầu điều chuyển hàng tồn</h3>\n",
    1264: "    else { toast('Đã chỉ định CH','success'); hideModal(); loadTransfers(); }\n",
}

for idx, replacement in fixes.items():
    old = lines[idx].strip()[:60]
    lines[idx] = replacement
    print(f"Fixed line {idx+1}: {old[:50]}... -> {replacement.strip()[:50]}...")

with open(filepath, 'w', encoding='utf-8', newline='\n') as f:
    f.writelines(lines)

print("Done! All 5 lines fixed.")

