package com.pb.plugins

import org.jetbrains.exposed.sql.Database

val database = Database.connect(
    url = "jdbc:mariadb://localhost:3306/PB_db", user = "pb-server", password = "123456", driver = "org.mariadb.jdbc.Driver"
)