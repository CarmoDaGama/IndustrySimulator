package com.industry.simulator.processing.dto;

import java.time.LocalDateTime;

public class ProcessingResponse {
    private Long id;
    private String batchId;
    private String materialName;
    private String materialType;
    private double quantity;
    private String unit;
    private String processingType;
    private long processingDurationMs;
    private String purpose;
    private boolean success;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public ProcessingResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getProcessingType() { return processingType; }
    public void setProcessingType(String processingType) { this.processingType = processingType; }
    public long getProcessingDurationMs() { return processingDurationMs; }
    public void setProcessingDurationMs(long processingDurationMs) { this.processingDurationMs = processingDurationMs; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ProcessingResponse response = new ProcessingResponse();

        public Builder id(Long id) { response.setId(id); return this; }
        public Builder batchId(String batchId) { response.setBatchId(batchId); return this; }
        public Builder materialName(String materialName) { response.setMaterialName(materialName); return this; }
        public Builder materialType(String materialType) { response.setMaterialType(materialType); return this; }
        public Builder quantity(double quantity) { response.setQuantity(quantity); return this; }
        public Builder unit(String unit) { response.setUnit(unit); return this; }
        public Builder processingType(String processingType) { response.setProcessingType(processingType); return this; }
        public Builder processingDurationMs(long duration) { response.setProcessingDurationMs(duration); return this; }
        public Builder purpose(String purpose) { response.setPurpose(purpose); return this; }
        public Builder success(boolean success) { response.setSuccess(success); return this; }
        public Builder errorMessage(String msg) { response.setErrorMessage(msg); return this; }
        public Builder createdAt(LocalDateTime time) { response.setCreatedAt(time); return this; }
        public Builder completedAt(LocalDateTime time) { response.setCompletedAt(time); return this; }
        
        public ProcessingResponse build() {
            return response;
        }
    }
}
