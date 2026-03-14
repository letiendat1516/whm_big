package org.example.service;

import org.example.dao.*;
import org.example.db.DatabaseManager;
import org.example.model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * POS Service — core checkout orchestration for VLXD.
 *
 * Flow (vật liệu xây dựng):
 *   1. Cashier scans barcode / tìm SP → getActivePrice() (frozen)
 *   2. addToCart()
 *   3. applyVoucher() (nếu có)
 *   4. checkout() — single transaction:
 *        a. completeOrder(total, discount, tax)
 *        b. decrementStock per item → SalesOutbound
 *        c. createPayment (partial or full)
 *        d. updatePaymentStatus (PAID/PARTIAL/UNPAID)
 *        e. earnPoints (if customer linked)
 *        f. Promotion log
 *
 * Khách VLXD có thể trả từng phần — debtAmount > 0 → PARTIAL.
 */
public class POSService {

    private final ProductDAO productDAO = new ProductDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final PromotionDAO promotionDAO = new PromotionDAO();
    private final DatabaseManager db = DatabaseManager.getInstance();

    private Order currentOrder;
    private final List<OrderItem> cartItems = new ArrayList<>();
    private double discountAmount = 0;
    private String appliedPromotionId = null;
    private String warehouseId;
    private double taxRate = 0.1; // 10% VAT mặc định

    public POSService() {}

    // ═══════════════════════════════════════════════════════════
    //  ORDER SESSION
    // ═══════════════════════════════════════════════════════════
    public void startOrder(String cashierId, String storeId, String shiftId) throws SQLException {
        cartItems.clear();
        discountAmount = 0;
        appliedPromotionId = null;
        pointsDiscount = 0;
        pointsRedeemed = 0;
        warehouseId = inventoryDAO.getDefaultWarehouseId(storeId);

        currentOrder = new Order();
        currentOrder.setOrderId(DatabaseManager.newId());
        currentOrder.setCashierId(cashierId);
        currentOrder.setStoreId(storeId);
        currentOrder.setShiftId(shiftId);
        currentOrder.setTaxRate(taxRate);
        currentOrder.setStatus("PENDING");
        orderDAO.createOrder(currentOrder);
    }

    public Order getCurrentOrder() { return currentOrder; }
    public List<OrderItem> getCartItems() { return cartItems; }
    public double getDiscountAmount() { return discountAmount; }

    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double rate) { this.taxRate = rate; }

    // ═══════════════════════════════════════════════════════════
    //  CART
    // ═══════════════════════════════════════════════════════════
    public OrderItem scanBarcode(String barcode) throws SQLException {
        if (currentOrder == null) throw new IllegalStateException("No active order.");
        ProductVariant variant = productDAO.findVariantByBarcode(barcode);
        if (variant == null) return null;
        double price = productDAO.getActivePrice(variant.getVariantId());
        if (price <= 0) throw new SQLException("Không có giá bán cho: " + variant.getVariantName());
        return buildOrderItem(variant, price, 1);
    }

    public OrderItem addVariantToCart(String variantId, int qty) throws SQLException {
        if (currentOrder == null) throw new IllegalStateException("No active order.");
        ProductVariant variant = productDAO.findVariantByBarcode(variantId);
        if (variant == null) {
            try (java.sql.PreparedStatement ps = db.getConnection().prepareStatement(
                     "SELECT pv.*, p.Name as product_name, p.product_code FROM ProductVariant pv " +
                     "JOIN Product p ON p.ProductID=pv.product_id WHERE pv.variant_id=? AND pv.status=1")) {
                ps.setString(1, variantId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    variant = new ProductVariant();
                    variant.setVariantId(rs.getString("variant_id"));
                    variant.setProductId(rs.getString("product_id"));
                    variant.setVariantName(rs.getString("variant_name"));
                    variant.setBarcode(rs.getString("barcode"));
                    variant.setProductName(rs.getString("product_name"));
                    variant.setProductCode(rs.getString("product_code"));
                }
            }
        }
        if (variant == null) return null;
        double price = productDAO.getActivePrice(variant.getVariantId());
        return buildOrderItem(variant, price, qty);
    }

    private OrderItem buildOrderItem(ProductVariant variant, double price, int qty) throws SQLException {
        OrderItem item = new OrderItem();
        item.setOrderItemId(DatabaseManager.newId());
        item.setOrderId(currentOrder.getOrderId());
        item.setProductId(variant.getProductId());
        item.setVariantId(variant.getVariantId());
        item.setProductName(variant.getProductName());
        item.setVariantName(variant.getVariantName());
        item.setBarcode(variant.getBarcode());
        item.setUnitPrice(price);
        item.setQuantity(qty);
        for (OrderItem existing : cartItems) {
            if (existing.getVariantId().equals(variant.getVariantId())) {
                existing.setQuantity(existing.getQuantity() + qty);
                orderDAO.addOrderItem(existing);
                return existing;
            }
        }
        cartItems.add(item);
        orderDAO.addOrderItem(item);
        return item;
    }

    public void removeFromCart(String orderItemId) throws SQLException {
        cartItems.removeIf(i -> i.getOrderItemId().equals(orderItemId));
        orderDAO.removeOrderItem(orderItemId);
    }

    /**
     * Apply per-item discount (giảm giá trên từng sản phẩm).
     * The item's subtotal becomes: quantity * unitPrice - itemDiscount.
     * Can still combine with order-level voucher.
     */
    public void setItemDiscount(String orderItemId, double discount) throws SQLException {
        for (OrderItem item : cartItems) {
            if (item.getOrderItemId().equals(orderItemId)) {
                double maxDiscount = item.getQuantity() * item.getUnitPrice();
                item.setItemDiscount(Math.min(discount, maxDiscount));
                orderDAO.updateItemDiscount(orderItemId, item.getItemDiscount());
                return;
            }
        }
    }

    /** Total per-item discounts across all cart items. */
    public double getTotalItemDiscounts() {
        return cartItems.stream().mapToDouble(OrderItem::getItemDiscount).sum();
    }

    // ═══════════════════════════════════════════════════════════
    //  POINTS REDEMPTION
    // ═══════════════════════════════════════════════════════════
    private double pointsDiscount = 0;
    private int    pointsRedeemed = 0;

    /**
     * Redeem customer loyalty points.
     * @param points number of points to redeem
     * @return the VND discount amount
     */
    public double redeemPoints(int points) throws SQLException {
        if (currentOrder == null) throw new IllegalStateException("No active order.");
        String custId = currentOrder.getCustomerId();
        if (custId == null || custId.isEmpty()) throw new SQLException("Chưa gắn khách hàng cho đơn.");
        java.sql.Connection conn = db.getConnection();
        pointsDiscount = customerDAO.redeemPoints(conn, custId, points, currentOrder.getOrderId());
        pointsRedeemed = points;
        return pointsDiscount;
    }

    public double getPointsDiscount() { return pointsDiscount; }
    public int    getPointsRedeemed() { return pointsRedeemed; }

    // ═══════════════════════════════════════════════════════════
    //  TOTALS (with tax)
    // ═══════════════════════════════════════════════════════════
    public double getCartTotal() {
        return cartItems.stream().mapToDouble(OrderItem::getSubtotal).sum();
    }

    public double getTaxAmount() {
        double afterDiscount = Math.max(0, getCartTotal() - discountAmount - pointsDiscount);
        return afterDiscount * taxRate;
    }

    public double getFinalAmount() {
        double afterDiscount = Math.max(0, getCartTotal() - discountAmount - pointsDiscount);
        return afterDiscount + getTaxAmount();
    }

    // ═══════════════════════════════════════════════════════════
    //  VOUCHER / PROMOTION
    // ═══════════════════════════════════════════════════════════
    public double applyVoucher(String voucherCode) throws SQLException {
        Promotion promo = promotionDAO.findByVoucherCode(voucherCode);
        if (promo == null) throw new SQLException("Voucher không hợp lệ hoặc đã hết hạn: " + voucherCode);
        discountAmount = promotionDAO.calculateDiscount(promo, getCartTotal(), cartItems.size());
        appliedPromotionId = promo.getPromotionId();
        return discountAmount;
    }

    public double applyAutoPromotion() throws SQLException {
        List<Promotion> promos = promotionDAO.findActivePromotions();
        for (Promotion p : promos) {
            if ("PERCENT_DISCOUNT".equals(p.getPromoType()) || "FIXED_DISCOUNT".equals(p.getPromoType())) {
                double d = promotionDAO.calculateDiscount(p, getCartTotal(), cartItems.size());
                if (d > 0) { discountAmount = d; appliedPromotionId = p.getPromotionId(); return d; }
            }
        }
        return 0;
    }

    public void setCustomer(String customerId) {
        if (currentOrder != null) currentOrder.setCustomerId(customerId);
    }

    // ═══════════════════════════════════════════════════════════
    //  CHECKOUT — partial payment support
    // ═══════════════════════════════════════════════════════════
    public CheckoutResult checkout(String paymentMethod, double cashReceived) throws SQLException {
        return checkout(paymentMethod, cashReceived, getFinalAmount());
    }

    /**
     * Checkout with explicit amountToPay.
     * If amountToPay < finalAmount → PARTIAL payment, debtAmount > 0.
     */
    public CheckoutResult checkout(String paymentMethod, double cashReceived, double amountToPay) throws SQLException {
        if (currentOrder == null || cartItems.isEmpty())
            throw new IllegalStateException("Không có đơn hàng hoặc giỏ hàng trống.");

        Connection conn = db.getConnection();
        conn.setAutoCommit(false);
        try {
            // 1. Complete order with tax
            orderDAO.completeOrder(conn, currentOrder.getOrderId(), discountAmount, taxRate);

            // 2. Decrement stock & create outbound
            if (warehouseId != null) {
                for (OrderItem item : cartItems) {
                    inventoryDAO.decrementStock(conn, warehouseId, item.getProductId(), item.getQuantity());
                }
                orderDAO.createSalesOutbound(conn, currentOrder.getOrderId(), warehouseId,
                        cartItems, currentOrder.getCashierId());
            }

            // 3. Payment — clamp amountToPay to finalAmount
            double finalAmt = getFinalAmount();
            double actualPay = Math.min(amountToPay, finalAmt);
            double change = "CASH".equals(paymentMethod) ? Math.max(0, cashReceived - actualPay) : 0;

            String paymentId = DatabaseManager.newId();
            orderDAO.createPayment(paymentId, currentOrder.getOrderId(), paymentMethod,
                    actualPay, cashReceived, change);

            // 4. Update payment status
            orderDAO.updatePaymentStatus(conn, currentOrder.getOrderId(), actualPay);

            // 5. Earn points
            int pointsEarned = 0;
            if (currentOrder.getCustomerId() != null && !currentOrder.getCustomerId().isEmpty()) {
                pointsEarned = customerDAO.earnPoints(conn, currentOrder.getCustomerId(),
                        currentOrder.getOrderId(), finalAmt);
            }

            // 6. Promotion log
            if (appliedPromotionId != null) {
                promotionDAO.applyPromotion(conn, appliedPromotionId, currentOrder.getOrderId(), discountAmount);
            }

            conn.commit();

            double debtRemaining = Math.max(0, finalAmt - actualPay);
            String payStatus = debtRemaining <= 0 ? "PAID" : (actualPay > 0 ? "PARTIAL" : "UNPAID");

            CheckoutResult result = new CheckoutResult(
                currentOrder.getOrderId(), finalAmt, discountAmount, pointsDiscount,
                getTaxAmount(), change, pointsEarned, paymentId, actualPay, debtRemaining, payStatus
            );

            cartItems.clear();
            currentOrder = null;
            discountAmount = 0;
            appliedPromotionId = null;
            pointsDiscount = 0;
            pointsRedeemed = 0;
            return result;

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  ADDITIONAL PAYMENT (trả nợ)
    // ═══════════════════════════════════════════════════════════
    public void payDebt(String orderId, String method, double amount) throws SQLException {
        Connection conn = db.getConnection();
        conn.setAutoCommit(false);
        try {
            String paymentId = DatabaseManager.newId();
            orderDAO.createPayment(paymentId, orderId, method, amount, amount, 0);
            orderDAO.updatePaymentStatus(conn, orderId, amount);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  RETURN ORDER
    // ═══════════════════════════════════════════════════════════
    public void processReturn(String orderId, List<ReturnOrderItem> items,
                               String managerId, String warehouseId) throws SQLException {
        ReturnOrder ro = new ReturnOrder();
        ro.setOrderId(orderId);
        ro.setManagerId(managerId);
        ro.setRestockWarehouseId(warehouseId);
        orderDAO.createReturnOrder(ro);

        for (ReturnOrderItem item : items) {
            item.setReturnId(ro.getReturnId());
            orderDAO.addReturnItem(item);
            if (warehouseId != null) {
                inventoryDAO.incrementStock(warehouseId, item.getProductId(), item.getQuantity());
            }
        }
        orderDAO.completeReturn(ro.getReturnId());
    }

    public void cancelCurrentOrder() throws SQLException {
        if (currentOrder != null) {
            orderDAO.cancelOrder(currentOrder.getOrderId(), "Cashier cancelled");
            cartItems.clear();
            currentOrder = null;
            discountAmount = 0;
        }
    }

    /** Immutable checkout result. */
    public record CheckoutResult(
        String orderId, double finalAmount, double discountAmount, double pointsDiscount,
        double taxAmount, double changeAmount, int pointsEarned, String paymentId,
        double paidAmount, double debtRemaining, String paymentStatus
    ) {}
}
