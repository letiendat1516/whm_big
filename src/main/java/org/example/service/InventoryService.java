package org.example.service;

import org.example.dao.InventoryDAO;
import org.example.model.Warehouse;
import org.example.model.WarehouseBalance;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/** Inventory management business logic. */
public class InventoryService {

    private final InventoryDAO dao = new InventoryDAO();

    public List<Warehouse> getWarehouses() throws SQLException {
        return dao.findAllWarehouses();
    }

    public List<WarehouseBalance> getAllStock() throws SQLException {
        return dao.findAllBalances();
    }

    public List<WarehouseBalance> getLowStockAlerts() throws SQLException {
        return dao.findLowStockBalances();
    }

    /** Receive goods into a warehouse (inbound). */
    public void receiveStock(String warehouseId, String productId, int qty) throws SQLException {
        if (qty <= 0) throw new IllegalArgumentException("Số lượng phải > 0");
        dao.incrementStock(warehouseId, productId, qty);
    }

    /** Get stock for a specific warehouse/product. */
    public WarehouseBalance getStockLevel(String warehouseId, String productId) throws SQLException {
        return dao.getBalance(warehouseId, productId);
    }

    /** Filter balances by warehouse. */
    public List<WarehouseBalance> getStockByWarehouse(String warehouseId) throws SQLException {
        return dao.findAllBalances().stream()
                  .filter(b -> warehouseId.equals(b.getWarehouseId()))
                  .collect(Collectors.toList());
    }
}

