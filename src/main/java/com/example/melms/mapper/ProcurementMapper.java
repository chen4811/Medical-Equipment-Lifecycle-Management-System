package com.example.melms.mapper;

import com.example.melms.pojo.ProcureOrder;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProcurementMapper {

    /* ---------- Admin: Accounts & Departments ---------- */

    @Select("""
            SELECT account_id AS id,
                   name,
                   department_id
            FROM tb_account
            ORDER BY account_id
            """)
    List<Map<String, Object>> listAccounts();

    @Select("""
            SELECT department_id AS id,
                   department_name AS name
            FROM tb_department
            ORDER BY department_id
            """)
    List<Map<String, Object>> listDepartments();

    /* ---------- Suppliers ---------- */

    @Select("""
            SELECT supplier_id AS id,
                   supplier_name AS name,
                   COALESCE(contact, '') AS contact
            FROM tb_supplier
            ORDER BY supplier_id
            """)
    List<Map<String, Object>> listVendors();

    // 生成下一个 4 位 supplier_id（0001 起）
    @Select("SELECT LPAD(COALESCE(MAX(CAST(supplier_id AS UNSIGNED)),0)+1, 4, '0') FROM tb_supplier")
    String nextSupplierId();

    @Insert("INSERT INTO tb_supplier (supplier_id, supplier_name, contact) VALUES (#{id}, #{name}, #{contact})")
    void addVendor(@Param("id") String id, @Param("name") String name, @Param("contact") String contact);

    /* ---------- Equipment Types ---------- */

    @Select("""
            SELECT equipment_type_id AS id,
                   equipment_type_name AS name
            FROM tb_equipment_type
            ORDER BY equipment_type_id
            """)
    List<Map<String, Object>> listEquipmentTypes();

    /* ---------- Quotes ---------- */

    @Select("""
            SELECT supplier_id AS supplier_id,
                   equipment_type_id AS equipment_type_id,
                   price
            FROM tb_supplier_equipment_type
            ORDER BY supplier_id, equipment_type_id
            """)
    List<Map<String, Object>> listQuotes();

    @Select("""
            SELECT COUNT(*) FROM tb_supplier_equipment_type
            WHERE supplier_id = #{supplier_id} AND equipment_type_id = #{equipment_type_id}
            """)
    Integer countQuote(@Param("supplier_id") String supplierId, @Param("equipment_type_id") String typeId);

    @Insert("""
            INSERT INTO tb_supplier_equipment_type (supplier_id, equipment_type_id, price)
            VALUES (#{supplier_id}, #{equipment_type_id}, #{price})
            """)
    void addQuote(@Param("supplier_id") String supplierId,
                  @Param("equipment_type_id") String typeId,
                  @Param("price") int price);

    @Update("""
            UPDATE tb_supplier_equipment_type
               SET price = #{price}
             WHERE supplier_id = #{supplier_id} AND equipment_type_id = #{equipment_type_id}
            """)
    void updateQuote(@Param("supplier_id") String supplierId,
                     @Param("equipment_type_id") String typeId,
                     @Param("price") int price);

    @Delete("""
            DELETE FROM tb_supplier_equipment_type
             WHERE supplier_id = #{supplier_id} AND equipment_type_id = #{equipment_type_id}
            """)
    void deleteQuote(@Param("supplier_id") String supplierId, @Param("equipment_type_id") String typeId);

    /* ---------- Orders ---------- */

    /**
     * 订单列表（仅返回 requester_id、department_id，交给前端二次映射）
     */
    @Select("""
            SELECT
                o.procure_id        AS procure_id,
                o.equipment_type_id AS equipment_type_id,
                o.count             AS count,
                o.supplier_id       AS supplier_id,
                o.status            AS status,
                o.requester_id      AS requester_id,
                a.department_id     AS department_id
            FROM tb_procure_order o
            LEFT JOIN tb_account a ON a.account_id = o.requester_id
            ORDER BY o.procure_id DESC
            """)
    List<Map<String, Object>> listOrders();

    // 当前状态查询（用于后端校验流程）
    @Select("SELECT status FROM tb_procure_order WHERE procure_id = #{id}")
    String findOrderStatus(@Param("id") int id);

    // 当前供应商查询（用于后端校验流程）
    @Select("SELECT supplier_id FROM tb_procure_order WHERE procure_id = #{id}")
    String findSupplierId(@Param("id") int id);

    @Update("UPDATE tb_procure_order SET status = #{status} WHERE procure_id = #{procure_id}")
    void updateOrderStatus(@Param("procure_id") int procureId, @Param("status") String status);

    // 允许把 supplier_id 置为 NULL；count 为空则保持原值
    @Update("""
            UPDATE tb_procure_order
               SET supplier_id = #{supplier_id},
                   count       = COALESCE(#{count}, count)
             WHERE procure_id  = #{procure_id}
            """)
    void assignOrder(@Param("procure_id") int procureId,
                     @Param("supplier_id") String supplierId,
                     @Param("count") Integer count);

    /* ---------- Extra (保留已有) ---------- */

    @Select("""
            SELECT
                o.procure_id AS procureId,
                o.equipment_type_id AS equipmentTypeId,
                t.equipment_type_name AS equipmentTypeName,
                o.count,
                o.supplier_id AS supplierId,
                s.supplier_name AS supplierName,
                o.status,
                o.requester_id AS requesterId,
                o.reason,
                o.created_at AS createdAt,
                o.updated_at AS updatedAt
            FROM tb_procure_order o
            LEFT JOIN tb_equipment_type t ON o.equipment_type_id = t.equipment_type_id
            LEFT JOIN tb_supplier s ON o.supplier_id = s.supplier_id
            WHERE o.status = 'arrived'
            """)
    List<ProcureOrder> getArrivedOrders();

    @Select("SELECT * FROM tb_procure_order WHERE requester_id = #{requesterId}")
    @Results({
            @Result(property = "procureId", column = "procure_id"),
            @Result(property = "equipmentTypeId", column = "equipment_type_id"),
            @Result(property = "count", column = "count"),
            @Result(property = "supplierId", column = "supplier_id"),
            @Result(property = "status", column = "status"),
            @Result(property = "requesterId", column = "requester_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "reason", column = "reason")
    })
    List<ProcureOrder> getProcureRequestsByDepartmentId(@Param("requesterId") String requesterId);

    @Insert("""
            INSERT INTO tb_procure_order
                (equipment_type_id, count, supplier_id, status, requester_id, reason)
            VALUES
                (#{equipmentTypeId}, #{count}, #{supplierId}, #{status}, #{requesterId}, #{reason})
            """)
    void insertProcureRequest(ProcureOrder procureOrder);

    @Update("UPDATE tb_procure_order SET status = #{status} WHERE procure_id = #{procureId}")
    void updateProcureStatus(@Param("procureId") String procureId, @Param("status") String status);
}
