package com.pb.dao.employees

import com.pb.enums.EmployeeRole
import com.pb.enums.EmployeeRole.Companion.parseEmployeeId
import com.pb.enums.EmployeeRole.Companion.table
import com.pb.models.Credentials
import com.pb.models.employee.ReceivedEmployee
import com.pb.models.employee.SavedEmployee
import com.pb.tables.AdminTable
import com.pb.tables.CoordinatorTable
import com.pb.tables.FreelancerTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class AdminTableDao(database: Database) {

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(
                AdminTable, CoordinatorTable, FreelancerTable
            )
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun verifyCredentials(credential: Credentials): Boolean {
        val password = try {
            dbQuery { AdminTable.select { AdminTable.employeeId eq credential.employeeId }.first()[AdminTable.password] }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return credential.password == password
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
            }.insertedCount
        } catch (e: Exception) {
            -1
        }
    }

    suspend fun getEmployeeById(employeeId: String) = dbQuery {
        val table = employeeId.parseEmployeeId().table
        return@dbQuery table.select(table.employeeId eq employeeId).firstOrNull()?.toSavedEmployee(table)
    }

    suspend fun getSelf(selfId: String) = dbQuery {
        return@dbQuery AdminTable.select { AdminTable.employeeId eq selfId }.firstOrNull()?.toSavedEmployee(AdminTable)
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

    suspend fun getEmployeeStatus(employeeId: String): Boolean? {
        try {
            val table = employeeId.parseEmployeeId().table
            return dbQuery { table.slice(table.availabilityStatus).select { table.employeeId eq employeeId }.first() }[table.availabilityStatus]
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun getAdminById(employeeId: String): SavedEmployee? {
        return if (!employeeId.startsWith("PB-AM")) null
        else getEmployeeById(employeeId)
    }

    suspend fun getCoordinatorById(employeeId: String): SavedEmployee? {
        return if (!employeeId.startsWith("PB-PC")) null
        else getEmployeeById(employeeId)
    }

    suspend fun getFreelancerById(employeeId: String): SavedEmployee? {
        return if (!employeeId.startsWith("PB-FR")) null
        else getEmployeeById(employeeId)
    }
}

