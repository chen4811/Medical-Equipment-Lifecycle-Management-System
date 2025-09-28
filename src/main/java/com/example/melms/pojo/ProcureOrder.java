package com.example.melms.pojo;
import lombok.Data;

import java.util.Date;

@Data

public class ProcureOrder {
    private int procureId;
    private String equipmentTypeId;
    private String equipmentTypeName;
    private int count;
    private String supplierId;
    private String supplierName;
    private String status;
    private int requesterId;
    private String reason;
    private Date createdAt;
    private Date updatedAt;
}
