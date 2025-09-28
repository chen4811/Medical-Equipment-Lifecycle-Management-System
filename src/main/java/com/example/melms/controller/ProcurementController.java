package com.example.melms.controller;

import com.example.melms.mapper.ProcurementMapper;
import com.example.melms.mapper.LogMapper;
import com.example.melms.pojo.ProcureOrder;
import com.example.melms.pojo.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class ProcurementController {

    @Resource
    private ProcurementMapper mapper;

    @Resource
    private LogMapper logMapper;

    @GetMapping("/req/proc/vendors")
    public Result listVendors() {
        try {
            List<Map<String, Object>> data = mapper.listVendors();
            return Result.success("ok", data);
        }
        catch (Exception e) {
            return Result.fail("500", e.getMessage(), null);
        }
    }

    @PostMapping("/req/proc/vendor")
    public Result createVendor(@RequestBody Map<String, String> payload) {
        try {
            String name = payload.get("supplier_name");
            String contact = payload.getOrDefault("contact", "");
            if (name == null || name.trim().isEmpty()) {
                return Result.fail("400", "Supplier name is required", null);
            }
            String newId = mapper.nextSupplierId();
            mapper.addVendor(newId, name.trim(), contact);

            try {
                String operatorId = payload.getOrDefault("operator_id", "0");
                logMapper.addNewLog("Add Supplier (supplierId=" + newId + ")", operatorId);
            }
            catch (Exception ignore) {
            }

            return Result.success("ok", Map.of("supplier_id", newId));
        }
        catch (Exception e) {
            return Result.fail("500", e.getMessage(), null);
        }
    }


    @GetMapping("/req/proc/equipmentTypes")
    public Result listEquipmentTypes() {
        try {
            return Result.success("ok", mapper.listEquipmentTypes());
        }
        catch (Exception e) {
            return Result.fail("500", e.getMessage(), null);
        }
    }

    @GetMapping("/req/proc/quotes")
    public Result listQuotes() {
        try {
            return Result.success("ok", mapper.listQuotes());
        }
        catch (Exception e) {
            return Result.fail("500", e.getMessage(), null);
        }
    }

    @PostMapping("/req/proc/quote")
    public Result addOrUpsertQuote(@RequestBody Map<String, Object> payload) {
        try {
            String supplierId = String.valueOf(payload.get("supplier_id"));
            String typeId = String.valueOf(payload.get("equipment_type_id"));
            Number priceNum = (Number) payload.getOrDefault("price", 0);
            int price = priceNum == null ? 0 : priceNum.intValue();

            if (supplierId == null || supplierId.isBlank() || typeId == null || typeId.isBlank()) {
                return Result.fail("400", "supplier_id & equipment_type_id are required", null);
            }

            Integer cnt = mapper.countQuote(supplierId, typeId);
            if (cnt != null && cnt > 0) {
                mapper.updateQuote(supplierId, typeId, price);
                try {
                    String operatorId = String.valueOf(payload.getOrDefault("operator_id", "0"));
                    logMapper.addNewLog("Edit Quote (supplierId=" + supplierId + ", typeId=" + typeId + ", price=" + price + ")", operatorId);
                }
                catch (Exception ignore) {
                }
            }
            else {
                mapper.addQuote(supplierId, typeId, price);
                try {
                    String operatorId = String.valueOf(payload.getOrDefault("operator_id", "0"));
                    logMapper.addNewLog("Add Quote (supplierId=" + supplierId + ", typeId=" + typeId + ", price=" + price + ")", operatorId);
                }
                catch (Exception ignore) {
                }
            }
            return Result.success("ok", null);
        }
        catch (Exception e) {
            return Result.fail("500", e.getMessage(), null);
        }
    }

    @PutMapping("/req/proc/quote")
    public Result updateQuote(@RequestBody Map<String, Object> payload) {
        try {
            String supplierId = String.valueOf(payload.get("supplier_id"));
            String typeId = String.valueOf(payload.get("equipment_type_id"));
            Number priceNum = (Number) payload.getOrDefault("price", 0);
            int price = priceNum == null ? 0 : priceNum.intValue();
            mapper.updateQuote(supplierId, typeId, price);

            // ➕ 日志
            try {
                String operatorId = String.valueOf(payload.getOrDefault("operator_id", "0"));
                logMapper.addNewLog("Edit Quote (supplierId=" + supplierId + ", typeId=" + typeId + ", price=" + price + ")", operatorId);
            }
            catch (Exception ignore) {
            }

            return Result.success("ok", null);
        }
        catch (Exception e) {
            return Result.fail("500", e.getMessage(), null);
        }
    }

    @DeleteMapping("/req/proc/quote")
    public Result deleteQuote(@RequestParam("supplierId") String supplierId,
                              @RequestParam("equipmentTypeId") String typeId,
                              @RequestParam(value = "operator_id", required = false, defaultValue = "0") String operatorId) {
        try {
            mapper.deleteQuote(supplierId, typeId);

            // ➕ 日志
            try {
                logMapper.addNewLog("Delete Quote (supplierId=" + supplierId + ", typeId=" + typeId + ")", operatorId);
            }
            catch (Exception ignore) {
            }

            return Result.success("ok", null);
        }
        catch (Exception e) {
            return Result.fail("500", e.getMessage(), null);
        }
    }

    @GetMapping("/req/proc/orders")
    public Result listOrders() {
        try {
            return Result.success("ok", mapper.listOrders());
        }
        catch (Exception e) {
            return Result.fail("500", e.getMessage(), null);
        }
    }

    @PutMapping("/req/proc/order/status")
    public Result changeOrderStatus(@RequestBody Map<String, String> payload) {
        try {
            int id = Integer.parseInt(payload.getOrDefault("procure_id", "0"));
            String status = payload.get("status");
            mapper.updateOrderStatus(id, status);

            // ➕ 日志
            try {
                String operatorId = payload.getOrDefault("operator_id", "0");
                logMapper.addNewLog("Change Procure Order Status (procureId=" + id + ", status=" + status + ")", operatorId);
            }
            catch (Exception ignore) {
            }

            return Result.success("ok", null);
        }
        catch (Exception e) {
            return Result.fail("500", e.getMessage(), null);
        }
    }

    @PutMapping("/req/proc/order/assign")
    public Result assignOrder(@RequestBody Map<String, Object> payload) {
        try {
            int id = Integer.parseInt(String.valueOf(payload.getOrDefault("procure_id", "0")));
            String supplierId = String.valueOf(payload.get("supplier_id"));
            int count = Integer.parseInt(String.valueOf(payload.getOrDefault("count", "0")));
            mapper.assignOrder(id, supplierId, count);

            try {
                String operatorId = String.valueOf(payload.getOrDefault("operator_id", "0"));
                logMapper.addNewLog("Assign Procure Order (procureId=" + id + ", supplierId=" + supplierId + ", count=" + count + ")", operatorId);
            }
            catch (Exception ignore) {
            }

            return Result.success("ok", null);
        }
        catch (Exception e) {
            return Result.fail("500", e.getMessage(), null);
        }
    }
}
