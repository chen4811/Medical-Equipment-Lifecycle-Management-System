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

    public Map<String, Object> getDashboardStats() {

        // 获取尚未确认的维修单数量（针对当前部门）
        // 假设当前部门ID在用户信息或上下文中获取
        String departmentId = "0001";  // 这里可以通过实际的上下文获取当前用户所在的部门
        int todosRepair = repairTicketMapper.countPendingRepairTickets(departmentId);
        // 获取设备统计信息
        int inUse = equipmentMapper.countDevicesByStatus("using", departmentId);  // 正在使用的设备数量
        int underRepair = equipmentMapper.countDevicesByStatus("repairing", departmentId);  // 维修中的设备数量

        // 获取今天的使用日志数量
        int todosUsage = logMapper.countLogsForToday(departmentId);

        // 返回统计数据
        return Map.of(
                "inUse", inUse,
                "underRepair", underRepair,
                "todosUsage", todosUsage,
                "todosRepair", todosRepair
        );
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

    public List<ProcureOrder> getProcureRequestsByDepartment(String departmentId) {
        return procurementMapper.getProcureRequestsByDepartmentId(departmentId);
    }

    // Create a new procure request
    public void createProcureRequest(ProcureOrder procureRequest) {
        procurementMapper.insertProcureRequest(procureRequest);
    }

    public void updateProcureStatus(String procureId, String status) {
        procurementMapper.updateProcureStatus(procureId, status);
    }
}




