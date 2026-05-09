package com.industry.simulator.rawmaterial.dto;

import java.time.LocalDateTime;

public class RawMaterialResponse {
    private String id;
    private String materialName;
    private String materialType;
    private double quantity;
    private String unit;
    private String batchId;
    private LocalDateTime createdAt;

    public RawMaterialResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RawMaterialResponse response = new RawMaterialResponse();

        public Builder id(String id) { response.setId(id); return this; }
        public Builder materialName(String name) { response.setMaterialName(name); return this; }
        public Builder materialType(String type) { response.setMaterialType(type); return this; }
        public Builder quantity(double quantity) { response.setQuantity(quantity); return this; }
        public Builder unit(String unit) { response.setUnit(unit); return this; }
        public Builder batchId(String batchId) { response.setBatchId(batchId); return this; }
        public Builder createdAt(LocalDateTime time) { response.setCreatedAt(time); return this; }
        
        public RawMaterialResponse build() {
            return response;
        }
    }
}
