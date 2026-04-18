package com.dentalclinic.model;

import java.math.BigDecimal;
import java.util.Date;

public class DentalService {
    private int serviceId;
    private String serviceName;
    private String description;
    private BigDecimal price;
    private boolean active;
    private Date createdAt;
    private Date updatedAt;

    public DentalService(int serviceId, String serviceName) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
    }

    public DentalService(int serviceId, String serviceName, String description, BigDecimal price, boolean active, Date createdAt, Date updatedAt) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.description = description;
        this.price = price;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getServiceId() { return serviceId; }
    public String getServiceName() { return serviceName; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public boolean isActive() { return active; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
}
