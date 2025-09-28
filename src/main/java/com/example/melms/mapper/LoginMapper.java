package com.example.melms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Map;

@Mapper
public interface LoginMapper {

    @Select("SELECT account_id, role FROM tb_account WHERE name = #{name} and password = #{password}")
    Map<String, Object> login(@Param("name") String name, @Param("password") String password);

    @Select("SELECT account_id, name, role, department_id FROM tb_account WHERE account_id = #{id}")
    Map<String, Object> findById(@Param("id") int id);

    @Select("SELECT account_id, name, role, department_id, email FROM tb_account WHERE email = #{email} LIMIT 1")
    Map<String, Object> findByEmail(@Param("email") String email);

    @Update("UPDATE tb_account SET password = #{password} WHERE email = #{email}")
    int updatePasswordByEmail(@Param("email") String email, @Param("password") String password);

    @Select("SELECT account_id FROM tb_account WHERE name = #{name} LIMIT 1")
    Integer findIdByName(@Param("name") String name);
}
