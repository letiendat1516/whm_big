package org.example.dao;

import org.example.db.DatabaseManager;
import org.example.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Order, OrderItem, Payment, Receipt, ReturnOrder, Campaign operations. */
public class OrderDAO {

    private final DatabaseManager db = DatabaseManager.getInstance();

    // ══════════════════════════════════════════════════════════════
    //  ORDER
    // ══════════════════════════════════════════════════════════════
    public void createOrder(Order o) throws SQLException {
        String sql = "INSERT INTO \"Order\"(orderId,cashierId,shiftId,storeId,customerId,status," +
                     "totalAmount,discountAmount,finalAmount,taxRate,taxAmount," +
                     "paymentStatus,paidAmount,debtAmount,note) " +
                     "VALUES(?,?,?,?,?,'PENDING',0,0,0,?,0,'UNPAID',0,0,?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, o.getOrderId()); ps.setString(2, o.getCashierId());
            ps.setString(3, o.getShiftId()); ps.setString(4, o.getStoreId());
            ps.setString(5, o.getCustomerId()); ps.setDouble(6, o.getTaxRate());
            ps.setString(7, o.getNote());
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("Order", o.getOrderId(), "INSERT", null);
    }

    public Order findById(String orderId) throws SQLException {
        String sql = "SELECT o.*, e.fullName as cashier_name, cu.FullName as customer_name " +
                     "FROM \"Order\" o " +
                     "LEFT JOIN Cashier c ON c.cashierId=o.cashierId " +
                     "LEFT JOIN Employee e ON e.employeeId=c.employeeId " +
                     "LEFT JOIN Customer cu ON cu.CustomerID=o.customerId " +
                     "WHERE o.orderId=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapOrder(rs) : null; }
        }
    }

    /**
     * Complete order — recalculate totals WITH TAX.
     * taxAmount = subtotal * taxRate
     * finalAmount = subtotal - discount + taxAmount
     */
    public void completeOrder(Connection conn, String orderId, double discountAmount, double taxRate) throws SQLException {
        String sumSql = "SELECT COALESCE(SUM(subtotal),0) as total FROM OrderItem WHERE orderId=?";
        double total;
        try (PreparedStatement ps = conn.prepareStatement(sumSql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) { total = rs.next() ? rs.getDouble("total") : 0; }
        }
        double afterDiscount = Math.max(0, total - discountAmount);
        double taxAmount = afterDiscount * taxRate;
        double finalAmt = afterDiscount + taxAmount;
        String upd = "UPDATE \"Order\" SET status='COMPLETED', totalAmount=?, discountAmount=?, " +
                     "taxRate=?, taxAmount=?, finalAmount=?, debtAmount=? WHERE orderId=?";
        try (PreparedStatement ps = conn.prepareStatement(upd)) {
            ps.setDouble(1, total); ps.setDouble(2, discountAmount);
            ps.setDouble(3, taxRate); ps.setDouble(4, taxAmount);
            ps.setDouble(5, finalAmt); ps.setDouble(6, finalAmt); // initially debtAmount = finalAmount
            ps.setString(7, orderId);
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("Order", orderId, "UPDATE", null);
    }

    /** Update payment tracking after a payment is recorded. */
    public void updatePaymentStatus(Connection conn, String orderId, double paidNow) throws SQLException {
        String sql = "UPDATE \"Order\" SET paidAmount = paidAmount + ?, " +
                     "debtAmount = finalAmount - (paidAmount + ?), " +
                     "paymentStatus = CASE " +
                     "  WHEN (paidAmount + ?) >= finalAmount THEN 'PAID' " +
                     "  WHEN (paidAmount + ?) > 0 THEN 'PARTIAL' " +
                     "  ELSE 'UNPAID' END " +
                     "WHERE orderId=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, paidNow); ps.setDouble(2, paidNow);
            ps.setDouble(3, paidNow); ps.setDouble(4, paidNow);
            ps.setString(5, orderId);
            ps.executeUpdate();
        }
    }

    public void cancelOrder(String orderId, String reason) throws SQLException {
        String sql = "UPDATE \"Order\" SET status='CANCELLED', note=? WHERE orderId=? AND status='PENDING'";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, reason); ps.setString(2, orderId); ps.executeUpdate();
        }
        db.addSyncQueueEntry("Order", orderId, "UPDATE", null);
    }

    public List<Order> findOrdersByDate(String date) throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, e.fullName as cashier_name, cu.FullName as customer_name " +
                     "FROM \"Order\" o " +
                     "LEFT JOIN Cashier c ON c.cashierId=o.cashierId " +
                     "LEFT JOIN Employee e ON e.employeeId=c.employeeId " +
                     "LEFT JOIN Customer cu ON cu.CustomerID=o.customerId " +
                     "WHERE date(o.orderDate)=? ORDER BY o.orderDate DESC";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapOrder(rs)); }
        }
        return list;
    }

    /** Find orders with outstanding debt (UNPAID or PARTIAL). */
    public List<Order> findDebtOrders() throws SQLException {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT o.*, e.fullName as cashier_name, cu.FullName as customer_name " +
                     "FROM \"Order\" o " +
                     "LEFT JOIN Cashier c ON c.cashierId=o.cashierId " +
                     "LEFT JOIN Employee e ON e.employeeId=c.employeeId " +
                     "LEFT JOIN Customer cu ON cu.CustomerID=o.customerId " +
                     "WHERE o.paymentStatus IN ('UNPAID','PARTIAL') AND o.status='COMPLETED' " +
                     "ORDER BY o.orderDate DESC";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapOrder(rs));
        }
        return list;
    }

    /** Search orders by ID prefix or customer name. */
    public List<Order> searchOrders(String query) throws SQLException {
        List<Order> list = new ArrayList<>();
        String like = "%" + (query == null ? "" : query) + "%";
        String sql = "SELECT o.*, e.fullName as cashier_name, cu.FullName as customer_name " +
                     "FROM \"Order\" o " +
                     "LEFT JOIN Cashier c ON c.cashierId=o.cashierId " +
                     "LEFT JOIN Employee e ON e.employeeId=c.employeeId " +
                     "LEFT JOIN Customer cu ON cu.CustomerID=o.customerId " +
                     "WHERE o.orderId LIKE ? OR cu.FullName LIKE ? OR o.status LIKE ? " +
                     "ORDER BY o.orderDate DESC LIMIT 100";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapOrder(rs)); }
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════════
    //  ORDER ITEM
    // ══════════════════════════════════════════════════════════════
    public void addOrderItem(OrderItem item) throws SQLException {
        String check = "SELECT orderItemId, quantity FROM OrderItem WHERE orderId=? AND variantId=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(check)) {
            ps.setString(1, item.getOrderId()); ps.setString(2, item.getVariantId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int newQty = rs.getInt("quantity") + item.getQuantity();
                    String upd = "UPDATE OrderItem SET quantity=?,subtotal=?*unitPrice WHERE orderItemId=?";
                    try (PreparedStatement pu = db.getConnection().prepareStatement(upd)) {
                        pu.setInt(1, newQty); pu.setInt(2, newQty); pu.setString(3, rs.getString("orderItemId"));
                        pu.executeUpdate();
                    }
                    return;
                }
            }
        }
        if (item.getOrderItemId() == null) item.setOrderItemId(DatabaseManager.newId());
        String sql = "INSERT INTO OrderItem(orderItemId,orderId,productId,variantId,quantity,unitPrice,subtotal) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, item.getOrderItemId()); ps.setString(2, item.getOrderId());
            ps.setString(3, item.getProductId());   ps.setString(4, item.getVariantId());
            ps.setInt(5, item.getQuantity());        ps.setDouble(6, item.getUnitPrice());
            ps.setDouble(7, item.getSubtotal());
            ps.executeUpdate();
        }
    }

    public void removeOrderItem(String orderItemId) throws SQLException {
        String sql = "DELETE FROM OrderItem WHERE orderItemId=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, orderItemId); ps.executeUpdate();
        }
    }

    /** Update per-item discount and recalculate subtotal. */
    public void updateItemDiscount(String orderItemId, double itemDiscount) throws SQLException {
        String sql = "UPDATE OrderItem SET itemDiscount=?, subtotal=(quantity*unitPrice-?) WHERE orderItemId=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setDouble(1, itemDiscount); ps.setDouble(2, itemDiscount);
            ps.setString(3, orderItemId); ps.executeUpdate();
        }
    }

    public List<OrderItem> findItemsByOrder(String orderId) throws SQLException {
        List<OrderItem> list = new ArrayList<>();
        String sql = "SELECT oi.*, p.Name as product_name, pv.variant_name, pv.barcode " +
                     "FROM OrderItem oi " +
                     "JOIN Product p ON p.ProductID=oi.productId " +
                     "JOIN ProductVariant pv ON pv.variant_id=oi.variantId " +
                     "WHERE oi.orderId=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapOrderItem(rs)); }
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════════
    //  PAYMENT (supports partial / multiple payments per order)
    // ══════════════════════════════════════════════════════════════
    public void createPayment(String paymentId, String orderId, String method, double amountPaid,
                               double cashReceived, double changeAmount) throws SQLException {
        Connection conn = db.getConnection();
        String sql = "INSERT INTO Payment(paymentId,orderId,method,amountPaid,status) VALUES(?,?,?,?,'COMPLETED')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paymentId); ps.setString(2, orderId);
            ps.setString(3, method); ps.setDouble(4, amountPaid);
            ps.executeUpdate();
        }
        if ("CASH".equals(method)) {
            String cashSql = "INSERT INTO CashPayment(paymentId,cashReceived,changeAmount) VALUES(?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(cashSql)) {
                ps.setString(1, paymentId); ps.setDouble(2, cashReceived); ps.setDouble(3, changeAmount);
                ps.executeUpdate();
            }
        }
        String receiptId = DatabaseManager.newId();
        String rcptSql = "INSERT INTO Receipt(receiptId,paymentId,format) VALUES(?,?,'THERMAL')";
        try (PreparedStatement ps = conn.prepareStatement(rcptSql)) {
            ps.setString(1, receiptId); ps.setString(2, paymentId); ps.executeUpdate();
        }
        db.addSyncQueueEntry("Payment", paymentId, "INSERT", null);
    }

    /** Get total already paid for an order. */
    public double getTotalPaid(String orderId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amountPaid),0) as total FROM Payment WHERE orderId=? AND status='COMPLETED'";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getDouble("total") : 0; }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  RETURN ORDER
    // ══════════════════════════════════════════════════════════════
    public void createReturnOrder(ReturnOrder ro) throws SQLException {
        if (ro.getReturnId() == null) ro.setReturnId(DatabaseManager.newId());
        String sql = "INSERT INTO ReturnOrder(returnId,orderId,managerId,status,quantity,restockWarehouseId) " +
                     "VALUES(?,?,?,'PENDING',0,?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, ro.getReturnId()); ps.setString(2, ro.getOrderId());
            ps.setString(3, ro.getManagerId()); ps.setString(4, ro.getRestockWarehouseId());
            ps.executeUpdate();
        }
        db.addSyncQueueEntry("ReturnOrder", ro.getReturnId(), "INSERT", null);
    }

    public void addReturnItem(ReturnOrderItem item) throws SQLException {
        if (item.getReturnItemId() == null) item.setReturnItemId(DatabaseManager.newId());
        item.setSubtotal(item.getQuantity() * item.getUnitPrice());
        String sql = "INSERT INTO ReturnOrderItem(returnItemId,returnId,orderItemId,productId,variantId,quantity,unitPrice,subtotal,reason) " +
                     "VALUES(?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, item.getReturnItemId()); ps.setString(2, item.getReturnId());
            ps.setString(3, item.getOrderItemId()); ps.setString(4, item.getProductId());
            ps.setString(5, item.getVariantId()); ps.setInt(6, item.getQuantity());
            ps.setDouble(7, item.getUnitPrice()); ps.setDouble(8, item.getSubtotal());
            ps.setString(9, item.getReason());
            ps.executeUpdate();
        }
    }

    public void completeReturn(String returnId) throws SQLException {
        String sql = "UPDATE ReturnOrder SET status='COMPLETED', returnDate=datetime('now') WHERE returnId=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, returnId); ps.executeUpdate();
        }
        // Update total quantity
        String qSql = "UPDATE ReturnOrder SET quantity=(SELECT COALESCE(SUM(quantity),0) FROM ReturnOrderItem WHERE returnId=?) WHERE returnId=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(qSql)) {
            ps.setString(1, returnId); ps.setString(2, returnId); ps.executeUpdate();
        }
        db.addSyncQueueEntry("ReturnOrder", returnId, "UPDATE", null);
    }

    public List<ReturnOrder> findAllReturns() throws SQLException {
        List<ReturnOrder> list = new ArrayList<>();
        String sql = "SELECT ro.*, cu.FullName as customer_name FROM ReturnOrder ro " +
                     "LEFT JOIN \"Order\" o ON o.orderId=ro.orderId " +
                     "LEFT JOIN Customer cu ON cu.CustomerID=o.customerId " +
                     "ORDER BY ro.returnDate DESC";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ReturnOrder r = new ReturnOrder();
                r.setReturnId(rs.getString("returnId")); r.setOrderId(rs.getString("orderId"));
                r.setManagerId(rs.getString("managerId")); r.setReturnDate(rs.getString("returnDate"));
                r.setStatus(rs.getString("status")); r.setQuantity(rs.getInt("quantity"));
                try { r.setRestockWarehouseId(rs.getString("restockWarehouseId")); } catch (SQLException ignored) {}
                try { r.setCustomerName(rs.getString("customer_name")); } catch (SQLException ignored) {}
                list.add(r);
            }
        }
        return list;
    }

    public List<ReturnOrderItem> findReturnItems(String returnId) throws SQLException {
        List<ReturnOrderItem> list = new ArrayList<>();
        String sql = "SELECT ri.*, p.Name as product_name, pv.variant_name " +
                     "FROM ReturnOrderItem ri " +
                     "JOIN Product p ON p.ProductID=ri.productId " +
                     "LEFT JOIN ProductVariant pv ON pv.variant_id=ri.variantId " +
                     "WHERE ri.returnId=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, returnId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReturnOrderItem ri = new ReturnOrderItem();
                    ri.setReturnItemId(rs.getString("returnItemId"));
                    ri.setReturnId(rs.getString("returnId"));
                    ri.setOrderItemId(rs.getString("orderItemId"));
                    ri.setProductId(rs.getString("productId"));
                    ri.setVariantId(rs.getString("variantId"));
                    ri.setQuantity(rs.getInt("quantity"));
                    ri.setUnitPrice(rs.getDouble("unitPrice"));
                    ri.setSubtotal(rs.getDouble("subtotal"));
                    ri.setReason(rs.getString("reason"));
                    try { ri.setProductName(rs.getString("product_name")); } catch (SQLException ignored) {}
                    try { ri.setVariantName(rs.getString("variant_name")); } catch (SQLException ignored) {}
                    list.add(ri);
                }
            }
        }
        return list;
    }

    // ══════════════════════════════════════════════════════════════
    //  SALES OUTBOUND (xuất kho gắn với đơn hàng)
    // ══════════════════════════════════════════════════════════════
    public void createSalesOutbound(Connection conn, String orderId, String warehouseId,
                                     List<OrderItem> items, String createdBy) throws SQLException {
        String outboundId = DatabaseManager.newId();
        String sql = "INSERT INTO SalesOutbound(outboundId,orderId,warehouseId,status,createdBy) VALUES(?,?,?,'POSTED',?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, outboundId); ps.setString(2, orderId);
            ps.setString(3, warehouseId); ps.setString(4, createdBy);
            ps.executeUpdate();
        }
        String itemSql = "INSERT INTO SalesOutboundItem(outboundItemId,outboundId,productId,variantId,quantity) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
            for (OrderItem item : items) {
                ps.setString(1, DatabaseManager.newId()); ps.setString(2, outboundId);
                ps.setString(3, item.getProductId()); ps.setString(4, item.getVariantId());
                ps.setInt(5, item.getQuantity());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        db.addSyncQueueEntry("SalesOutbound", outboundId, "INSERT", null);
    }

    // ══════════════════════════════════════════════════════════════
    //  CAMPAIGN CRUD
    // ══════════════════════════════════════════════════════════════
    public List<Campaign> findAllCampaigns() throws SQLException {
        List<Campaign> list = new ArrayList<>();
        String sql = "SELECT * FROM Campaign ORDER BY StartDate DESC";
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Campaign c = new Campaign();
                c.setCampaignId(rs.getString("CampaignID"));
                c.setName(rs.getString("Name"));
                c.setDescription(rs.getString("Description"));
                c.setStartDate(rs.getString("StartDate"));
                c.setEndDate(rs.getString("EndDate"));
                c.setIsActive(rs.getInt("IsActive"));
                try { c.setStoreId(rs.getString("storeId")); } catch (SQLException ignored) {}
                list.add(c);
            }
        }
        return list;
    }

    public void saveCampaign(Campaign c) throws SQLException {
        boolean isNew = c.getCampaignId() == null || c.getCampaignId().isEmpty();
        if (isNew) c.setCampaignId(DatabaseManager.newId());
        if (isNew) {
            String sql = "INSERT INTO Campaign(CampaignID,Name,Description,StartDate,EndDate,IsActive,storeId) VALUES(?,?,?,?,?,?,?)";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, c.getCampaignId()); ps.setString(2, c.getName());
                ps.setString(3, c.getDescription()); ps.setString(4, c.getStartDate());
                ps.setString(5, c.getEndDate()); ps.setInt(6, c.getIsActive());
                ps.setString(7, c.getStoreId());
                ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE Campaign SET Name=?,Description=?,StartDate=?,EndDate=?,IsActive=?,storeId=? WHERE CampaignID=?";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, c.getName()); ps.setString(2, c.getDescription());
                ps.setString(3, c.getStartDate()); ps.setString(4, c.getEndDate());
                ps.setInt(5, c.getIsActive()); ps.setString(6, c.getStoreId());
                ps.setString(7, c.getCampaignId());
                ps.executeUpdate();
            }
        }
        db.addSyncQueueEntry("Campaign", c.getCampaignId(), isNew ? "INSERT" : "UPDATE", null);
    }

    public void deleteCampaign(String campaignId) throws SQLException {
        String sql = "DELETE FROM Campaign WHERE CampaignID=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, campaignId); ps.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PROMOTION / VOUCHER CRUD
    // ══════════════════════════════════════════════════════════════
    public void savePromotion(Promotion p) throws SQLException {
        boolean isNew = p.getPromotionId() == null || p.getPromotionId().isEmpty();
        if (isNew) p.setPromotionId(DatabaseManager.newId());
        if (isNew) {
            String sql = "INSERT INTO Promotion(PromotionID,CampaignID,PromoType,Name,Priority," +
                         "RuleDefinition,VoucherCode,MaxUsageCount,ExpiryDate,TriggerCondition,IsActive) " +
                         "VALUES(?,?,?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, p.getPromotionId()); ps.setString(2, p.getCampaignId());
                ps.setString(3, p.getPromoType()); ps.setString(4, p.getName());
                ps.setInt(5, p.getPriority()); ps.setString(6, p.getRuleDefinition());
                ps.setString(7, p.getVoucherCode()); ps.setInt(8, p.getMaxUsageCount());
                ps.setString(9, p.getExpiryDate()); ps.setString(10, p.getTriggerCondition());
                ps.setInt(11, p.getIsActive());
                ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE Promotion SET CampaignID=?,PromoType=?,Name=?,Priority=?," +
                         "RuleDefinition=?,VoucherCode=?,MaxUsageCount=?,ExpiryDate=?," +
                         "TriggerCondition=?,IsActive=? WHERE PromotionID=?";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, p.getCampaignId()); ps.setString(2, p.getPromoType());
                ps.setString(3, p.getName()); ps.setInt(4, p.getPriority());
                ps.setString(5, p.getRuleDefinition()); ps.setString(6, p.getVoucherCode());
                ps.setInt(7, p.getMaxUsageCount()); ps.setString(8, p.getExpiryDate());
                ps.setString(9, p.getTriggerCondition()); ps.setInt(10, p.getIsActive());
                ps.setString(11, p.getPromotionId());
                ps.executeUpdate();
            }
        }
        db.addSyncQueueEntry("Promotion", p.getPromotionId(), isNew ? "INSERT" : "UPDATE", null);
    }

    public void deletePromotion(String promotionId) throws SQLException {
        String sql = "DELETE FROM Promotion WHERE PromotionID=?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, promotionId); ps.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  MAPPERS
    // ══════════════════════════════════════════════════════════════
    private Order mapOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setOrderId(rs.getString("orderId")); o.setCashierId(rs.getString("cashierId"));
        o.setShiftId(rs.getString("shiftId")); o.setStoreId(rs.getString("storeId"));
        o.setCustomerId(rs.getString("customerId")); o.setOrderDate(rs.getString("orderDate"));
        o.setStatus(rs.getString("status")); o.setTotalAmount(rs.getDouble("totalAmount"));
        o.setDiscountAmount(rs.getDouble("discountAmount")); o.setFinalAmount(rs.getDouble("finalAmount"));
        o.setNote(rs.getString("note"));
        try { o.setTaxRate(rs.getDouble("taxRate")); } catch (SQLException ignored) {}
        try { o.setTaxAmount(rs.getDouble("taxAmount")); } catch (SQLException ignored) {}
        try { o.setPaymentStatus(rs.getString("paymentStatus")); } catch (SQLException ignored) {}
        try { o.setPaidAmount(rs.getDouble("paidAmount")); } catch (SQLException ignored) {}
        try { o.setDebtAmount(rs.getDouble("debtAmount")); } catch (SQLException ignored) {}
        try { o.setCashierName(rs.getString("cashier_name")); } catch (SQLException ignored) {}
        try { o.setCustomerName(rs.getString("customer_name")); } catch (SQLException ignored) {}
        return o;
    }

    private OrderItem mapOrderItem(ResultSet rs) throws SQLException {
        OrderItem i = new OrderItem();
        i.setOrderItemId(rs.getString("orderItemId")); i.setOrderId(rs.getString("orderId"));
        i.setProductId(rs.getString("productId")); i.setVariantId(rs.getString("variantId"));
        i.setQuantity(rs.getInt("quantity")); i.setUnitPrice(rs.getDouble("unitPrice"));
        try { i.setItemDiscount(rs.getDouble("itemDiscount")); } catch (SQLException ignored){}
        i.setSubtotal(rs.getDouble("subtotal"));
        try { i.setProductName(rs.getString("product_name")); } catch (SQLException ignored) {}
        try { i.setVariantName(rs.getString("variant_name")); } catch (SQLException ignored) {}
        try { i.setBarcode(rs.getString("barcode")); } catch (SQLException ignored) {}
        return i;
    }
}

