package com.pb.dao

import com.pb.models.Credentials
import com.pb.models.employee.EmployeeRole
import com.pb.models.employee.EmployeeRole.Companion.parseEmployeeId
import com.pb.models.employee.EmployeeRole.Companion.table
import com.pb.models.employee.ReceivedEmployee
import com.pb.models.employee.SavedEmployee
import com.pb.tables.AdminTable
import com.pb.tables.CoordinatorTable
import com.pb.tables.EmployeeTable
import com.pb.tables.FreelancerTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class EmployeeTableDao(database: Database) {

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(
                AdminTable, CoordinatorTable, FreelancerTable
            )
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun verifyCredentials(credential: Credentials) = dbQuery {
        val table = credential.employeeId.parseEmployeeId().table
        val password = table.select(table.employeeId eq credential.employeeId).firstOrNull()?.get(table.password) ?: return@dbQuery false
        return@dbQuery credential.password == password
    }

    suspend fun createEmployee(newEmployee: ReceivedEmployee) = dbQuery {
        try {
            val table = newEmployee.role.table

            return@dbQuery table.insert { statement ->
                statement[employeeId] = newEmployee.employeeId
                statement[name] = newEmployee.name
                statement[emailAddress] = newEmployee.emailAddress
                statement[contactNumber] = newEmployee.contactNumber
                statement[availabilityStatus] = newEmployee.availabilityStatus
                statement[role] = newEmployee.role
            }[table.employeeId]
        } catch (e: Exception) {
            null
        }
    }

    suspend fun setEmployeeStatus(employeeId: String, status: Boolean) = dbQuery {
        val role = employeeId.parseEmployeeId()
        role.table.update({ role.table.employeeId eq employeeId }) { it[this.availabilityStatus] = status }
    }

    suspend fun getEmployeeStatus(employeeId: String): Boolean? {
        return getEmployeeById(employeeId)?.availabilityStatus
    }

    suspend fun getEmployeeById(employeeId: String) = dbQuery {
        val table = employeeId.parseEmployeeId().table
        return@dbQuery table.select(table.employeeId eq employeeId).firstOrNull()?.toSavedEmployee(table)
    }

    suspend fun getEmployeesByRole(role: EmployeeRole) = dbQuery {
        val table = role.table
        return@dbQuery table.select(table.role eq role).map { it.toSavedEmployee(table) }
    }

    suspend fun removeEmployeeById(employeeId: String) = dbQuery {
        val table = employeeId.parseEmployeeId().table
        return@dbQuery table.deleteWhere { table.employeeId eq employeeId }
    }

    suspend fun removeFreelancerById(employeeId: String) = dbQuery {
        return@dbQuery if (!employeeId.startsWith("PB-FR")) null
        else removeEmployeeById(employeeId)
    }

    suspend fun removeCoordinatorById(employeeId: String) = dbQuery {
        return@dbQuery if (!employeeId.startsWith("PB-PC")) null
        else removeEmployeeById(employeeId)
    }

    suspend fun getCoordinatorById(employeeId: String): SavedEmployee? {
        return if (!employeeId.startsWith("PB-PC")) null
        else getEmployeeById(employeeId)
    }


    suspend fun getFreelancerById(employeeId: String): SavedEmployee? {
        return if (!employeeId.startsWith("PB-FR")) null
        else getEmployeeById(employeeId)
    }

    private fun ResultRow.toSavedEmployee(table: EmployeeTable): SavedEmployee {
        return SavedEmployee(get(table.employeeId), get(table.name), get(table.emailAddress), get(table.contactNumber), get(table.availabilityStatus), get(table.role))
    }

}

