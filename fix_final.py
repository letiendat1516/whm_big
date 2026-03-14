# -*- coding: utf-8 -*-
import re

f = r'src\main\resources\public\dashboard.html'
data = open(f, 'r', encoding='utf-8').read()

# Map of garbled UTF-8-read-as-CP1252 -> correct Vietnamese
# These are the REMAINING garbled strings after previous fixes
replacements = {
    # Emojis (multi-byte garbled)
    'ðŸ"„': '🔄',
    'ðŸ"': '🔍',
    'ðŸ—'ï¸': '🗑️',
    'ðŸ›'': '🛒',
    'ðŸ"‹': '📋',
    'ðŸ·ï¸': '🏷️',
    'ðŸ­': '🏭',
    'ðŸŽ': '🎁',
    'ðŸ—ï¸': '🏗️',
    'â†©ï¸': '↩️',
    'âœ…': '✅',

    # Common Vietnamese words/phrases (longer first)
    'Tráº¡ng thÃ¡i Ä\u0091á»\u0093ng bá»™': 'Trạng thái đồng bộ',
    'Äang kiá»\u0083m tra...': 'Đang kiểm tra...',
    'ÄÃ£ táº£i láº¡i dá»¯ liá»\u0087u': 'Đã tải lại dữ liệu',
    'Táº¡o yÃªu cáº§u Ä\u0091iá»\u0081u chuyá»\u0083n hÃ ng tá»\u0093n': 'Tạo yêu cầu điều chuyển hàng tồn',
    'ÄÃ£ chá»\u0089 Ä\u0091á»\u008bnh kho': 'Đã chỉ định kho',
    'ÄÃ£ chá»\u0089 Ä\u0091á»\u008bnh CH': 'Đã chỉ định CH',
    'Giáº£m tiá»\u0081n cá»\u0091 Ä\u0091á»\u008bnh': 'Giảm tiền cố định',
    'Thanh toÃ¡n má»\u0099t pháº§n': 'Thanh toán một phần',
    'Sá»\u0091 tiá»\u0081n khÃ¡ch tráº£': 'Số tiền khách trả',
    'Sá»\u0091 tiá»\u0081n thanh toÃ¡n': 'Số tiền thanh toán',
    'QR / Chuyá»\u0083n khoáº£n': 'QR / Chuyển khoản',
    'Tiá»\u0081n máº·t': 'Tiền mặt',
    'Giá» hÃ ng trá»\u0091ng': 'Giỏ hàng trống',
    'Giá» trá»\u0091ng': 'Giỏ trống',
    'ÄÆ¡n vá»\u008b tÃ­nh': 'Đơn vị tính',
    'Äá»\u008ba chá»\u0089': 'Địa chỉ',
    'Äá»\u008ba chá»\u0089 kho': 'Địa chỉ kho',
    'Äang lÃ m': 'Đang làm',
    'Nghá»\u0089 viá»\u0087c': 'Nghỉ việc',
    'Tráº¡ng thÃ¡i': 'Trạng thái',
    'Sá»\u0091 tiá»\u0081n': 'Số tiền',
    'Sá»\u0091 tiá»\u0081n hoÃ n': 'Số tiền hoàn',
    'PhÆ°Æ¡ng thá»©c': 'Phương thức',
    'ThÆ°Æ¡ng hiá»\u0087u': 'Thương hiệu',
    'thÆ°Æ¡ng hiá»\u0087u': 'thương hiệu',

    # Table headers
    'MÃ£ SP': 'Mã SP',
    'MÃ£ ÄH': 'Mã ĐH',
    'MÃ£ TT': 'Mã TT',
    'MÃ£ kho': 'Mã kho',
    'MÃ£ tráº£': 'Mã trả',
    'MÃ£ ÄH gá»\u0091c': 'Mã ĐH gốc',
    'MÃ£ voucher': 'Mã voucher',
    'MÃ£': 'Mã',
    'TÃªn sáº£n pháº©m': 'Tên sản phẩm',
    'TÃªn kho': 'Tên kho',
    'TÃªn': 'Tên',
    'tÃªn': 'tên',
    'ÄVT': 'ĐVT',
    'GiÃ¡': 'Giá',
    'Tá»\u0093n': 'Tồn',
    'Tá»\u0093n kho': 'Tồn kho',
    'NgÃ y': 'Ngày',
    'Tá»\u0095ng tiá»\u0081n': 'Tổng tiền',
    'Thuáº¿': 'Thuế',
    'Giáº£m giÃ¡': 'Giảm giá',
    'ThÃ nh tiá»\u0081n': 'Thành tiền',
    'Thanh toÃ¡n': 'Thanh toán',
    'CÃ´ng ná»£': 'Công nợ',
    'Thao tÃ¡c': 'Thao tác',
    'LÃ½ do': 'Lý do',
    'Há»\u008d tÃªn': 'Họ tên',
    'SÄT': 'SĐT',
    'Háº¡ng': 'Hạng',
    'Äiá»\u0083m': 'Điểm',
    'NgÃ y ÄK': 'Ngày ĐK',
    'Loáº¡i': 'Loại',
    'Æ¯u tiÃªn': 'Ưu tiên',
    'ÄÃ£ dÃ¹ng/Tá»\u0091i Ä\u0091a': 'Đã dùng/Tối đa',
    'Háº¿t háº¡n': 'Hết hạn',
    'KÃ­ch hoáº¡t': 'Kích hoạt',
    'Sáº£n pháº©m': 'Sản phẩm',
    'sáº£n pháº©m': 'sản phẩm',
    'ÄÃ£ Ä\u0091áº·t': 'Đã đặt',
    'Kháº£ dá»¥ng': 'Khả dụng',
    'Danh má»¥c': 'Danh mục',
    'danh má»¥c': 'danh mục',

    # Search placeholders
    'TÃ¬m sáº£n pháº©m (tÃªn, mÃ£)': 'Tìm sản phẩm (tên, mã)',
    'TÃ¬m sáº£n pháº©m (tÃªn, mÃ£, thÆ°Æ¡ng hiá»\u0087u)': 'Tìm sản phẩm (tên, mã, thương hiệu)',
    'TÃ¬m Ä\u0091Æ¡n hÃ ng': 'Tìm đơn hàng',
    'TÃ¬m thanh toÃ¡n': 'Tìm thanh toán',
    'TÃ¬m nhÃ  cung cáº¥p': 'Tìm nhà cung cấp',
    'TÃ¬m khÃ¡ch hÃ ng (tÃªn, SÄT, email)': 'Tìm khách hàng (tên, SĐT, email)',
    'TÃ¬m nhÃ¢n viÃªn (tÃªn, mÃ£)': 'Tìm nhân viên (tên, mã)',
    'TÃ¬m': 'Tìm',

    # Buttons and labels
    'XÃ³a giá»\u008f': 'Xóa giỏ',
    'XÃ³a': 'Xóa',
    'Giá»\u008f hÃ ng': 'Giỏ hàng',
    'ÄÆ¡n hÃ ng': 'Đơn hàng',
    'Ä\u0091Æ¡n hÃ ng': 'đơn hàng',
    'BÃ¡n hÃ ng': 'Bán hàng',
    'Tráº£ hÃ ng': 'Trả hàng',
    'Kho hÃ ng': 'Kho hàng',
    'NhÃ  cung cáº¥p': 'Nhà cung cấp',
    'nhÃ  cung cáº¥p': 'nhà cung cấp',
    'KhÃ¡ch hÃ ng': 'Khách hàng',
    'khÃ¡ch hÃ ng': 'khách hàng',
    'Khuyáº¿n mÃ£i': 'Khuyến mãi',
    'NhÃ¢n sá»±': 'Nhân sự',
    'TÃ i khoáº£n': 'Tài khoản',
    'tÃ i khoáº£n': 'tài khoản',
    'ÄÄƒng xuáº¥t': 'Đăng xuất',
    'Ä\u0091Äƒng nháº­p': 'đăng nhập',
    'Táº£i láº¡i': 'Tải lại',
    'NhÃ¢n viÃªn': 'Nhân viên',
    'nhÃ¢n viÃªn': 'nhân viên',
    'ÄÃ£': 'Đã',

    # Misc remaining
    'mÃ£': 'mã',
    'hÃ ng': 'hàng',
    'HÃ ng': 'Hàng',
    'VD: bao, kg, cÃ¡i': 'VD: bao, kg, cái',
}

# Sort by length (longest first) to avoid partial replacements
sorted_replacements = sorted(replacements.items(), key=lambda x: -len(x[0]))

for garbled, correct in sorted_replacements:
    data = data.replace(garbled, correct)

open(f, 'w', encoding='utf-8', newline='\n').write(data)

# Quick verification
lines = data.split('\n')
garbled_count = 0
for i, line in enumerate(lines, 1):
    # Check for common garbled patterns
    if any(c in line for c in ['Ã¡', 'Ã©', 'Ã³', 'áº', 'á»', 'Æ°', 'Äá»']):
        garbled_count += 1
        if garbled_count <= 20:
            print(f'Line {i}: {line.strip()[:120]}')

print(f'\nTotal lines with potential garbled text: {garbled_count}')
print('Done!')

