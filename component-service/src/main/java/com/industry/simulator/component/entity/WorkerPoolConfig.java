package com.industry.simulator.component.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "worker_pool_config")
public class WorkerPoolConfig {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private int workerCount = 2;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getWorkerCount() { return workerCount; }
    public void setWorkerCount(int workerCount) { this.workerCount = workerCount; }
}
