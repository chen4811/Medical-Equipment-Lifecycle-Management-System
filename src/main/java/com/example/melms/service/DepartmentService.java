package com.example.melms.service;

import com.example.melms.mapper.*;
import com.example.melms.pojo.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class DepartmentService {
    @Resource
    private DepartmentMapper departmentMapper;

    @Resource
    private EquipmentMapper equipmentMapper;

    @Resource
    private ProcurementMapper procurementMapper;

    @Resource
    private LogMapper logMapper;

    @Resource
    private RepairTicketMapper repairTicketMapper;

    public List<Department> listAll() {
        return departmentMapper.findAll();
    }

    public Department getById(Integer id) {
        return departmentMapper.findById(id);
    }

    public int add(Department department) {
        return departmentMapper.insert(department);
    }

    public int update(Department department) {
        return departmentMapper.update(department);
    }

    public int delete(Integer id) {
        return departmentMapper.delete(id);
    }

    public Object getDepartmentId(Long accountId) {
        return departmentMapper.findDepartmentIdByAccountId(accountId);
    }

    public Map<String, Object> getDashboardStats(String departmentId) {
        int todosRepair = repairTicketMapper.countPendingRepairTickets(departmentId);

        int inUse = equipmentMapper.countDevicesByStatus("using", departmentId);  // 正在使用的设备数量
        int underRepair = equipmentMapper.countDevicesByStatus("repairing", departmentId);  // 维修中的设备数量
        int todosUsage = logMapper.countLogsForToday(departmentId);

        return Map.of(
                "inUse", inUse,
                "underRepair", underRepair,
                "todosUsage", todosUsage,
                "todosRepair", todosRepair
        );
    }

    public List<UsageLog> getUsageLogsByRecorderId(String recorderId) {
        return departmentMapper.findByRecorderId(recorderId);
    }

    public List<UsageLog> getUsageLogsByEquipmentId(String equipmentId) {
        return departmentMapper.findByTargetEquipmentId(equipmentId);
    }

    public boolean addUsageLog(UsageLog usageLog) {
        try {
            // 使用 MyBatis 或 JPA 进行插入
            departmentMapper.insertUsageLog(usageLog);  // 如果使用 MyBatis
            // 或者
            // usageLoggingRepository.save(usageLog);  // 如果使用 JPA
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public List<RepairTicket> getRepairLogs(String equipmentId) {
        return repairTicketMapper.getRepairLogsByEquipmentId(equipmentId);
    }

    public List<RepairTicket> getRepairLogsByDepartment(String departmentId) {
        return repairTicketMapper.getRepairLogsByDepartmentId(departmentId);
    }

    public String generateNewTicketId() {
        String maxTicketId = repairTicketMapper.getMaxTicketId(); // 查询最大 ticket_id
        int nextId = 1;

        if (maxTicketId != null) {
            String numberPart = maxTicketId.substring(1);
            nextId = Integer.parseInt(numberPart) + 1;
        }

        // 格式化新 ticket_id，保持 4 位数字格式
        return String.format("T%04d", nextId);
    }

    public void createRepairTicket(RepairTicket repairTicket) {
        String newTicketId = generateNewTicketId();
        repairTicket.setTicketId(newTicketId);

        repairTicketMapper.insertRepairTicket(repairTicket);
    }

    public List<ProcureOrder> getProcureRequestsByDepartment(String requesterId) {
        return procurementMapper.getProcureRequestsByDepartmentId(requesterId);
    }

    // Create a new procure request
    public void createProcureRequest(ProcureOrder procureRequest) {
        procurementMapper.insertProcureRequest(procureRequest);
    }

    public void updateProcureStatus(String procureId, String status) {
        procurementMapper.updateProcureStatus(procureId, status);
    }
}




