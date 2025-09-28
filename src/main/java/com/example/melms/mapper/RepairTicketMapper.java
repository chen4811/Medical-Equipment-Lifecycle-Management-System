package com.example.melms.mapper;

import com.example.melms.pojo.RepairTicket;
import org.apache.ibatis.annotations.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface RepairTicketMapper {

    @Select("SELECT t.*, d.department_name AS departmentName " +
            "FROM tb_repair_ticket t " +
            "JOIN tb_department d ON t.department_id = d.department_id")
    List<RepairTicket> findAll();

    @Update("UPDATE tb_repair_ticket " +
            "SET notes=#{notes}, cost=#{cost}, result=#{result}, status=#{status}, finished_at=#{finishedAt} " +
            "WHERE ticket_id=#{ticketId}")
    int update(RepairTicket ticket);

    @Select("SELECT * FROM tb_repair_ticket WHERE ticket_id=#{id}")
    RepairTicket findById(String id);

    @Select("""
        SELECT status, COUNT(*) AS cnt 
        FROM tb_repair_ticket 
        GROUP BY status
    """)
    java.util.List<Map<String, Object>> countTodayTicketsRaw();

    default Map<String, Integer> countTodayTickets() {
        var list = countTodayTicketsRaw();
        Map<String, Integer> map = new HashMap<>();
        map.put("pendingResponse", 0);
        map.put("inProgress", 0);
        map.put("pendingAcceptance", 0);

        for (Map<String,Object> m : list) {
            String status = (String)m.get("status");
            int count = ((Number)m.get("cnt")).intValue();
            switch(status) {
                case "pending" -> map.put("pendingResponse", count);
                case "in-progress" -> map.put("inProgress", count);
                case "under-acceptance" -> map.put("pendingAcceptance", count);
            }
        }
        return map;
    }

    @Update("UPDATE tb_repair_ticket SET status = #{status}, updated_at = NOW() WHERE ticket_id = #{ticketId}")
    int updateStatus(@Param("ticketId") int ticketId, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM tb_repair_ticket WHERE status = 'In Repair' AND department_id = #{departmentId}")
    int countPendingRepairTickets(String departmentId);

    @Select("SELECT * FROM tb_repair_ticket WHERE equipment_id = #{equipmentId}")
    @Results({
            @Result(property = "ticketId", column = "ticket_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "finishedAt", column = "finished_at"),
            @Result(property = "notes", column = "notes"),
            @Result(property = "cost", column = "cost"),
            @Result(property = "result", column = "result"),
            @Result(property = "status", column = "status"),
            @Result(property = "departmentId", column = "department_id"),
            @Result(property = "requesterId", column = "requester_id"),
            @Result(property = "managerId", column = "manager_id"),
            @Result(property = "equipmentId", column = "equipment_id")
    })
    List<RepairTicket> getRepairLogsByEquipmentId(@Param("equipmentId") String equipmentId);

    @Select("SELECT * FROM tb_repair_ticket WHERE department_id = #{departmentId}")
    @Results({
            @Result(property = "ticketId", column = "ticket_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "finishedAt", column = "finished_at"),
            @Result(property = "notes", column = "notes"),
            @Result(property = "cost", column = "cost"),
            @Result(property = "result", column = "result"),
            @Result(property = "status", column = "status"),
            @Result(property = "departmentId", column = "department_id"),
            @Result(property = "requesterId", column = "requester_id"),
            @Result(property = "managerId", column = "manager_id"),
            @Result(property = "equipmentId", column = "equipment_id")
    })
    List<RepairTicket> getRepairLogsByDepartmentId(@Param("departmentId") String departmentId);

    @Select("SELECT MAX(ticket_id) FROM tb_repair_ticket")
    String getMaxTicketId();

    @Insert("INSERT INTO tb_repair_ticket (ticket_id, created_at, notes, cost, result, status, department_id, requester_id, manager_id, equipment_id) " +
            "VALUES (#{ticketId}, NOW(), #{notes}, #{cost}, #{result}, #{status}, #{departmentId}, #{requesterId}, #{managerId}, #{equipmentId})")
    void insertRepairTicket(RepairTicket repairTicket);

}
