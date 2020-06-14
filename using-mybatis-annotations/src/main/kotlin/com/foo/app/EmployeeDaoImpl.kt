package com.foo.app

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer
import org.springframework.stereotype.Repository

@Repository("employeeDaoImpl")
class EmployeeDaoImpl(val employeeMapper: EmployeeMapper, flywayMigrationInitializer: FlywayMigrationInitializer) : EmployeeDao by employeeMapper

/*
    Dependency on  FlywayMigrationInitializer : to ensure that flyway migration is done before somebody uses dao in their
    postconstructs. eg CacheService.
    Fix for this is available at springboot 2.3.0 M3
    Reference :
    https://github.com/spring-projects/spring-boot/commit/48819253eb5171b93ede2b630fdb8e64ca845e62
    https://github.com/spring-projects/spring-boot/issues/13155
*/