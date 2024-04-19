package com.pb

import com.pb.Constants.defaultEmployeePassword
import com.pb.enums.EmployeeRole
import com.pb.models.employee.SavedEmployee
import com.pb.plugins.MongoDb
import com.pb.plugins.configureAuthorizedDbAccess
import com.pb.plugins.configureSecurity
import com.pb.plugins.configureSerialization
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain.main(args)
}

fun Application.module() {
/*    runBlocking {
        MongoDb.mongoDb.getCollection<SavedEmployee>("Admin").insertOne(
            SavedEmployee("PB-AM-A001", "Mohit", "mohit@example.com", defaultEmployeePassword, "12345", false, EmployeeRole.ADMIN)
        )
    }*/

    configureSecurity()
    configureSerialization()
    configureAuthorizedDbAccess()
}
