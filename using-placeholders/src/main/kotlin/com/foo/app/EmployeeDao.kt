package com.foo.app

interface EmployeeDao {
    fun getEmployee(id : Int) : Employee
}