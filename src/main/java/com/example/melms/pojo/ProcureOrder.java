package com.example.melms.pojo;
import lombok.Data;

import java.util.Date;

@Data

public class ProcureOrder {
    private String procureId;
    private String equipmentTypeId;
    private Integer count;
    private String supplierId;
    private String status;
    private String requesterId;
    private String reason;
    private Date createdAt;
    private Date updatedAt;
}
