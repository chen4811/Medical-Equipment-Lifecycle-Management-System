package com.example.melms.controller;

import com.example.melms.pojo.Equipment;
import com.example.melms.pojo.ProcureOrder;
import com.example.melms.pojo.RepairTicket;
import com.example.melms.pojo.UsageLog;
import com.example.melms.service.DepartmentService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/req")
@CrossOrigin
public class DepartmentController {

    @Resource
    private DepartmentService departmentService;

    @GetMapping("/department/id")
    public Object getDepartmentByAccountId(@RequestParam("accountId") Long accountId) {
        return departmentService.getDepartmentId(accountId);
    }

    @GetMapping("/dept/dashboard/stats")
    public Object getDashboardStats(@RequestParam("departmentId") String departmentId) {
        return departmentService.getDashboardStats(departmentId);
    }

    @GetMapping("/dept/usage/logs")
    public List<UsageLog> getUsageLogs(@RequestParam("recorderId") String recorderId) {
        return departmentService.getUsageLogsByEquipmentId(recorderId);
    }

    @PostMapping("/dept/usage/logs")
    public String addUsageLog(@RequestBody UsageLog usageLog) {
        boolean success = departmentService.addUsageLog(usageLog);
        if (success) {
            return "Usage log added successfully!";
        } else {
            return "Failed to add usage log.";
        }
    }

    // 获取设备的维修记录
    @GetMapping("/dept/repair/logs")
    public List<RepairTicket> getRepairLogs(@RequestParam("equipmentId") String equipmentId) {
        return departmentService.getRepairLogs(equipmentId);
    }

    @GetMapping("/dept/repair/logs/id")
    public List<RepairTicket> getRepairLogsByDepartment(@RequestParam("departmentId") String departmentId) {
        return departmentService.getRepairLogsByDepartment(departmentId);
    }

    // 创建新的维修单
    @PostMapping("/dept/repair/logs")
    public void createRepairTicket(@RequestBody RepairTicket repairTicket) {
        departmentService.createRepairTicket(repairTicket);
    }

    @GetMapping("/dept/procure/logs")
    public List<ProcureOrder> getProcureRequests(@RequestParam("requesterId") String requesterId) {
        return departmentService.getProcureRequestsByDepartment(requesterId);
    }

    // Create a new procurement request
    @PostMapping("/dept/procure/logs")
    public void createProcureRequest(@RequestBody ProcureOrder procureRequest) {
        departmentService.createProcureRequest(procureRequest);
    }

    @PutMapping("/dept/procure/logs/{procureId}")
    public ResponseEntity<String> updateProcureStatus(@PathVariable String procureId, @RequestBody Map<String, String> requestBody) {
        String status = requestBody.get("status");
        departmentService.updateProcureStatus(procureId, status);

        return ResponseEntity.ok("Status updated successfully.");
    }
}
