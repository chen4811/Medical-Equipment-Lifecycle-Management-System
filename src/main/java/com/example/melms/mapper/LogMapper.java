package com.example.melms.mapper;

import com.example.melms.pojo.Log;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LogMapper {
    @Select("SELECT log_id, log_time, log_action, log_user_id FROM tb_log")
    @Results({
            @Result(property = "log_id", column = "log_id"),
            @Result(property = "log_time", column = "log_time"),
            @Result(property = "log_action", column = "log_action"),
            @Result(property = "log_user_id", column = "log_user_id")
    })
    Log[] getLogs();

    @Insert("INSERT INTO tb_log (log_action, log_user_id) VALUES (#{log_action}, #{log_user_id})")
    void addNewLog(@Param("log_action") String log_action, @Param("log_user_id") String log_user_id);

    @Select("SELECT COUNT(*) FROM tb_usage_logging l " +
            "JOIN tb_equipment e ON l.target_equipment_id = e.equipment_id " +
            "WHERE DATE(l.time) = CURDATE() " +
            "AND e.department_id = #{departmentId}")
    int countLogsForToday(@Param("departmentId") String departmentId);
}
