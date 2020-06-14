package com.foo.app

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Select

@Mapper
interface EmployeeMapper : EmployeeDao {
    @Select("SELECT id, name FROM employee WHERE id=#{id}")
    override fun getEmployee(id: Int): Employee
}