package org.example.service;

import org.example.dao.CustomerDAO;
import org.example.model.Customer;

import java.sql.SQLException;
import java.util.List;

/** CRM: customer lookup and loyalty management. */
public class CRMService {

    private final CustomerDAO dao = new CustomerDAO();

    public Customer lookupByPhone(String phone) throws SQLException {
        return dao.findByPhone(phone);
    }

    public List<Customer> searchCustomers(String query) throws SQLException {
        return dao.searchCustomers(query);
    }

    public void saveCustomer(Customer c) throws SQLException {
        if (c.getFullName() == null || c.getFullName().isBlank())
            throw new IllegalArgumentException("Họ tên không được để trống.");
        if (c.getPhoneNum() == null || c.getPhoneNum().isBlank())
            throw new IllegalArgumentException("Số điện thoại không được để trống.");
        // Prevent duplicate phone
        if (c.getCustomerId() == null || c.getCustomerId().isEmpty()) {
            Customer existing = dao.findByPhone(c.getPhoneNum());
            if (existing != null) throw new IllegalArgumentException("Số điện thoại đã được đăng ký.");
        }
        dao.saveCustomer(c);
    }

    public List<Object[]> getPointHistory(String customerId) throws SQLException {
        return dao.getPointHistory(customerId);
    }
}

