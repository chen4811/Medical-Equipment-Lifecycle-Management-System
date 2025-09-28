package com.example.melms.service;

import com.example.melms.mapper.LogMapper;
import com.example.melms.mapper.RepairTicketMapper;
import com.example.melms.pojo.RepairTicket;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RepairTicketService {
    @Resource
    private RepairTicketMapper mapper;
    @Resource
    RepairTicketMapper repairTicketMapper;

    @Resource
    LogMapper logMapper;

    public List<RepairTicket> getAll() {
        return mapper.findAll();
    }

    public boolean save(RepairTicket ticket) {
        return mapper.update(ticket) > 0;
    }

    public boolean complete(String ticketId) {
        RepairTicket ticket = mapper.findById(ticketId);
        if (ticket == null) return false;
        ticket.setStatus("Completed");
        ticket.setFinishedAt(new java.util.Date());
        return mapper.update(ticket) > 0;
    }

    public boolean advanceStatus(int ticketId, String status, String userId) {
        int updated = repairTicketMapper.updateStatus(ticketId, status);
        if (updated > 0) {

            logMapper.addNewLog("Advance repair ticket " + ticketId + " to " + status, userId);
            return true;
        }
        return false;
    }
}
