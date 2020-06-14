package com.foo.app

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class CacheService  {

    lateinit var employee: Employee

    @Autowired
    private lateinit var employeeDaoImpl: EmployeeDaoImpl

    @PostConstruct
    fun setup() {
        employee = this.employeeDaoImpl.getEmployee(1)
    }
}
