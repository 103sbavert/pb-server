package com.pb.dao.employees

import com.pb.models.employee.SavedEmployee
import com.pb.tables.EmployeeTable
import org.jetbrains.exposed.sql.ResultRow



internal fun ResultRow.toSavedEmployee(table: EmployeeTable): SavedEmployee {
    return SavedEmployee(get(table.employeeId), get(table.name), get(table.emailAddress), get(table.contactNumber), get(table.availabilityStatus), get(table.role))
}
