package com.foo.app

import org.mybatis.spring.annotation.MapperScan
import org.springframework.context.annotation.Configuration

@Configuration
@MapperScan(basePackages = ["com.foo.app"])
class AppConfig