package com.example.melms.pojo;
import lombok.Data;
@Data
public class Equipment {
    private int equipmentId;
    private String equipmentTypeId;
    private String status;
    private String userManualPath;
    private String warrantyCertificatePath;
    private String supplierId;
    private String supplierName;
    private int departmentId;
    private String picUrl;

    private String equipmentTypeName;
    private String departmentName;
}


