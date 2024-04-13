package com.pb.tables

import com.pb.models.employee.EmployeeRole
import org.jetbrains.exposed.sql.Table

sealed class EmployeeTable(name: String) : Table(name) {
    private val id = integer("id").autoIncrement()
    val employeeId = varchar("employee_id", length = 12).uniqueIndex()
    val name = varchar("name", 100)
    val emailAddress = varchar("email_address", 100)
    val contactNumber = varchar("contact_number", 16)
    val password = varchar("password", 32).default("1234")
    val role = enumeration<EmployeeRole>("role")
    val availabilityStatus = bool("availability_status").default(false)
    override val primaryKey = PrimaryKey(id)
}

object AdminTable : EmployeeTable("Admins") {

    init {
        this.role.default(EmployeeRole.ADMIN)
    }
}

object CoordinatorTable : EmployeeTable("Coordinators") {

    init {
        this.role.default(EmployeeRole.COORDINATOR)
    }
}

object FreelancerTable : EmployeeTable("Freelancers") {

    init {
        this.role.default(EmployeeRole.FREELANCER)
    }
}
