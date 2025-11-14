package io.nebula.rpc.grpc.test;

import java.util.List;
import java.util.Map;

/**
 * 复杂用户对象 - 用于测试嵌套对象序列化/反序列化
 */
public class ComplexUser {
    private Long id;
    private String name;
    private Address address;           // 嵌套对象
    private List<Order> orders;        // 嵌套对象列表
    private Map<String, String> metadata; // Map
    
    public ComplexUser() {
    }
    
    public ComplexUser(Long id, String name, Address address, List<Order> orders, Map<String, String> metadata) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.orders = orders;
        this.metadata = metadata;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Address getAddress() {
        return address;
    }
    
    public void setAddress(Address address) {
        this.address = address;
    }
    
    public List<Order> getOrders() {
        return orders;
    }
    
    public void setOrders(List<Order> orders) {
        this.orders = orders;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * 地址对象
     */
    public static class Address {
        private String street;
        private String city;
        private String zipCode;
        
        public Address() {
        }
        
        public Address(String street, String city, String zipCode) {
            this.street = street;
            this.city = city;
            this.zipCode = zipCode;
        }
        
        public String getStreet() {
            return street;
        }
        
        public void setStreet(String street) {
            this.street = street;
        }
        
        public String getCity() {
            return city;
        }
        
        public void setCity(String city) {
            this.city = city;
        }
        
        public String getZipCode() {
            return zipCode;
        }
        
        public void setZipCode(String zipCode) {
            this.zipCode = zipCode;
        }
    }
    
    /**
     * 订单对象
     */
    public static class Order {
        private String orderId;
        private Double amount;
        private String status;
        
        public Order() {
        }
        
        public Order(String orderId, Double amount, String status) {
            this.orderId = orderId;
            this.amount = amount;
            this.status = status;
        }
        
        public String getOrderId() {
            return orderId;
        }
        
        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }
        
        public Double getAmount() {
            return amount;
        }
        
        public void setAmount(Double amount) {
            this.amount = amount;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
    }
}

