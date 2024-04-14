package com.pb.dao.employees


import com.pb.models.Credentials
import com.pb.models.employee.SavedEmployee
import com.pb.tables.CoordinatorTable
import com.pb.tables.FreelancerTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class FreelancerTableDao(database: Database) {

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(
                FreelancerTable
            )
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun verifyCredentials(credential: Credentials): Boolean {
        val password = try {
            dbQuery { FreelancerTable.select { FreelancerTable.employeeId eq credential.employeeId }.first()[FreelancerTable.password] }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return credential.password == password
    }

    suspend fun getSelf(selfId: String): SavedEmployee? {
        try {
            return dbQuery { FreelancerTable.select { FreelancerTable.employeeId eq selfId }.first().toSavedEmployee(FreelancerTable) }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun setStatus(selfId: String, status: Boolean): Boolean {
        try {
            dbQuery { FreelancerTable.update({ FreelancerTable.employeeId eq selfId }) { it[this.availabilityStatus] = status } }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }


    suspend fun getStatus(employeeId: String): Boolean {
        return dbQuery { FreelancerTable.slice(FreelancerTable.availabilityStatus).select { FreelancerTable.employeeId eq employeeId }.first()[FreelancerTable.availabilityStatus] }
    }
}