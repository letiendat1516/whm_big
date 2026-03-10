# 🚀 Hướng dẫn Deploy lên Railway

## Tổng quan kiến trúc

```
┌─────────────────────────────────────────────┐
│            Railway Cloud                     │
│  ┌───────────────┐  ┌───────────────────┐   │
│  │  Java Web App │  │   PostgreSQL DB   │   │
│  │  (Javalin)    │──│  (Railway Addon)  │   │
│  │  Port: $PORT  │  │  DATABASE_URL     │   │
│  └───────────────┘  └───────────────────┘   │
│        ↑                                     │
└────────|─────────────────────────────────────┘
         │ HTTPS
   ┌─────┴─────┐
   │ Máy bán   │  Trình duyệt web
   │ hàng 1    │  (Chrome/Edge)
   └───────────┘
   ┌───────────┐
   │ Máy kho   │  Trình duyệt web
   │ hàng 2    │  (Chrome/Edge)
   └───────────┘
```

Mỗi máy chỉ cần mở trình duyệt → nhập URL → đăng nhập bằng account riêng.
Dữ liệu sync realtime qua PostgreSQL trung tâm.

---

## Bước 1: Cài đặt Git (nếu chưa có)

```bash
# Kiểm tra đã cài chưa
git --version

# Nếu chưa có, tải tại: https://git-scm.com/download/win
```

## Bước 2: Tạo tài khoản Railway

1. Truy cập https://railway.app
2. Đăng ký bằng **GitHub** (nên dùng GitHub)
3. Xác nhận email

## Bước 3: Push code lên GitHub

```bash
# Mở terminal (PowerShell) tại thư mục project:
cd C:\Users\utube\IdeaProjects\whm_big

# Khởi tạo git repo (nếu chưa có)
git init
git add .
git commit -m "Initial: Store Management Web App"

# Tạo repo trên GitHub.com → lấy URL
# Ví dụ: https://github.com/username/whm_big.git
git remote add origin https://github.com/YOUR_USERNAME/whm_big.git
git branch -M main
git push -u origin main
```

## Bước 4: Deploy trên Railway

### 4a. Tạo Project mới
1. Vào https://railway.app/dashboard
2. Click **"New Project"**
3. Chọn **"Deploy from GitHub repo"**
4. Chọn repo `whm_big`

### 4b. Thêm PostgreSQL Database
1. Trong project, click **"+ New"** → **"Database"** → **"Add PostgreSQL"**
2. Railway tự tạo DB và set biến `DATABASE_URL`
3. **QUAN TRỌNG**: Railway tự link `DATABASE_URL` vào service Java

### 4c. Cấu hình (đã có sẵn)
Project đã có:
- `Dockerfile` - build & run container
- `railway.toml` - cấu hình Railway
- Health check tại `/api/health`

### 4d. Deploy
- Railway tự động build khi bạn push code mới
- Hoặc click **"Deploy"** thủ công trong dashboard
- Chờ khoảng 2-3 phút build Docker image

### 4e. Lấy URL
1. Vào **Settings** → **Networking**
2. Click **"Generate Domain"** → Railway tạo URL dạng:
   `https://whm-big-production.up.railway.app`
3. Hoặc thêm custom domain nếu có

---

## Bước 5: Sử dụng

### Đăng nhập
Mở trình duyệt → nhập URL Railway → trang login hiện ra.

**Tài khoản mặc định (mật khẩu: `123456`):**

| Username    | Vai trò          | Quyền truy cập                     |
|------------|------------------|--------------------------------------|
| `admin`     | ADMIN            | Toàn bộ hệ thống                   |
| `cashier1`  | CASHIER          | Bán hàng + Thanh toán               |
| `warehouse1`| WAREHOUSE        | Kho hàng                            |
| `hr1`       | HR_MANAGER       | Nhân sự                             |
| `manager1`  | STORE_MANAGER    | Bán hàng + Kho + Sản phẩm + CRM    |

### Phân quyền theo vai trò
- **Thu ngân (CASHIER)**: Chỉ thấy tab Bán hàng + Thanh toán
- **Quản lý kho (WAREHOUSE)**: Chỉ thấy tab Kho hàng
- **Nhân sự (HR_MANAGER)**: Chỉ thấy tab Nhân sự
- **Quản lý cửa hàng (STORE_MANAGER)**: Bán hàng + Kho + Sản phẩm + CRM
- **Admin (ADMIN)**: Toàn bộ + Quản lý tài khoản

### Sync giữa các máy
Khi thu ngân tạo đơn bán hàng → máy quản lý kho sẽ thấy tồn kho cập nhật ngay
(vì tất cả đều truy cập cùng 1 PostgreSQL trên Railway)

---

## Bước 6: Quản lý sau deploy

### Xem logs
```bash
# Cài Railway CLI
npm install -g @railway/cli

# Login
railway login

# Xem logs
railway logs
```

### Cập nhật code
```bash
git add .
git commit -m "update: mô tả thay đổi"
git push origin main
# Railway tự động redeploy
```

### Biến môi trường
Railway tự set `DATABASE_URL` và `PORT`. Không cần thay đổi gì.

Nếu muốn thêm biến tùy chỉnh:
1. Railway Dashboard → Service → Variables
2. Thêm biến mới

---

## Chi phí Railway

| Plan     | Giá         | Phù hợp                   |
|----------|-------------|----------------------------|
| Trial    | $5 miễn phí | Test / dev                 |
| Hobby    | $5/tháng    | Production nhỏ (<50 users) |
| Pro      | $20/tháng   | Production lớn             |

PostgreSQL trên Railway: **miễn phí 1GB** (đủ cho ~100K đơn hàng).

---

## Troubleshooting

### 1. Build failed
- Kiểm tra logs trong Railway Dashboard
- Đảm bảo Dockerfile đúng (đã có sẵn)

### 2. Health check failed
- Health check endpoint: `GET /api/health`
- Timeout: 300s (đủ cho cold start)

### 3. Database error
- Kiểm tra `DATABASE_URL` đã được set chưa
- Vào Railway → PostgreSQL service → Data tab để kiểm tra tables

### 4. Đổi mật khẩu mặc định
Đăng nhập bằng `admin` → vào tab **Tài khoản** → sửa password

---

## Cấu trúc API

| Endpoint              | Method | Mô tả                  |
|----------------------|--------|--------------------------|
| `/api/auth/login`     | POST   | Đăng nhập               |
| `/api/auth/logout`    | POST   | Đăng xuất               |
| `/api/auth/me`        | GET    | Thông tin user hiện tại  |
| `/api/products`       | GET    | Danh sách sản phẩm      |
| `/api/orders`         | GET/POST| Đơn hàng               |
| `/api/payments`       | POST   | Thanh toán               |
| `/api/returns`        | GET    | Trả hàng                |
| `/api/warehouses`     | GET    | Danh sách kho            |
| `/api/inventory/balances` | GET| Tồn kho                 |
| `/api/customers`      | GET/POST| Khách hàng             |
| `/api/employees`      | GET/POST| Nhân viên              |
| `/api/accounts`       | GET/POST| Tài khoản              |
| `/api/health`         | GET    | Health check             |

