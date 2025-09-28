package com.example.melms.controller;

import com.example.melms.pojo.RepairTicket;
import com.example.melms.service.RepairTicketService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/req/repair-tickets")
public class RepairTicketController {
    @Resource
    private RepairTicketService service;

    @Resource
    RepairTicketService repairTicketService;

    @GetMapping
    public List<RepairTicket> list() {
        return service.getAll();
    }

    @PutMapping("/{id}")
    public String save(@PathVariable String id, @RequestBody RepairTicket ticket) {
        ticket.setTicketId(id);
        return service.save(ticket) ? "Updated" : "Failed";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable String id) {
        return service.complete(id) ? "Completed" : "Failed";
    }

    @PostMapping("/{ticketId}/advance")
    public ResponseEntity<Map<String, Object>> advanceStatus(
            @PathVariable("ticketId") int ticketId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String status = body.get("status");
        if (status == null || status.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
        }

        String userId = request.getHeader("X-User-Id");

        boolean ok = repairTicketService.advanceStatus(ticketId, status, userId);

        if (ok) {
            return ResponseEntity.ok(Map.of("result", "success", "status", status));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Ticket not found or update failed"));
        }
    }
}
