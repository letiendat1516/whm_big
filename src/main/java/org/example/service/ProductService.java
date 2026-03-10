package org.example.service;

import org.example.dao.ProductDAO;
import org.example.model.*;

import java.sql.SQLException;
import java.util.List;

/** Product catalog and price management. */
public class ProductService {

    private final ProductDAO dao = new ProductDAO();

    public List<Product> searchProducts(String query) throws SQLException {
        return dao.searchProducts(query);
    }

    public List<Product> getAllActive() throws SQLException {
        return dao.findAllActive();
    }

    public void saveProduct(Product p) throws SQLException {
        if (p.getName() == null || p.getName().isBlank())
            throw new IllegalArgumentException("Tên sản phẩm không được để trống.");
        if (p.getProductCode() == null || p.getProductCode().isBlank())
            throw new IllegalArgumentException("Mã sản phẩm không được để trống.");
        dao.saveProduct(p);
    }

    public List<ProductCategory> getCategories() throws SQLException {
        return dao.findAllCategories();
    }

    public void saveCategory(ProductCategory c) throws SQLException {
        if (c.getCategoryName() == null || c.getCategoryName().isBlank())
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        dao.saveCategory(c);
    }

    public List<ProductVariant> getVariants(String productId) throws SQLException {
        return dao.findVariantsByProduct(productId);
    }

    public void saveVariant(ProductVariant v) throws SQLException {
        if (v.getVariantName() == null || v.getVariantName().isBlank())
            throw new IllegalArgumentException("Tên biến thể không được để trống.");
        dao.saveVariant(v);
    }

    /** Set a new retail price for a variant. Closes the previous open-ended price. */
    public void setPriceRetail(String variantId, double price) throws SQLException {
        if (price <= 0) throw new IllegalArgumentException("Giá phải > 0");
        dao.setNewPrice(variantId, "PL-001", price);
    }

    public List<ProductPrice> getPriceHistory(String variantId) throws SQLException {
        return dao.findPriceHistory(variantId);
    }

    public double getActivePrice(String variantId) throws SQLException {
        return dao.getActivePrice(variantId);
    }
}

