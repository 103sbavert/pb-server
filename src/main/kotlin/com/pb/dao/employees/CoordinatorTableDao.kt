package com.pb.dao.employees


import com.pb.models.Credentials
import com.pb.models.employee.SavedEmployee
import com.pb.tables.CoordinatorTable
import com.pb.tables.FreelancerTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class CoordinatorTableDao(database: Database) {

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(
                CoordinatorTable
            )
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun verifyCredentials(credential: Credentials): Boolean {
        val password = try {
            dbQuery { CoordinatorTable.select { CoordinatorTable.employeeId eq credential.employeeId }.first()[CoordinatorTable.password] }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return credential.password == password
    }

    suspend fun getSelf(selfId: String): SavedEmployee? {
        try {
            return dbQuery { CoordinatorTable.select { CoordinatorTable.employeeId eq selfId }.first().toSavedEmployee(CoordinatorTable) }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun setStatus(selfId: String, status: Boolean): Boolean {
        try {
            dbQuery { CoordinatorTable.update({ CoordinatorTable.employeeId eq selfId }) { it[this.availabilityStatus] = status } }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }


    suspend fun getStatus(selfId: String): Boolean {
        return dbQuery { CoordinatorTable.slice(CoordinatorTable.availabilityStatus).select { CoordinatorTable.employeeId eq selfId }.first()[CoordinatorTable.availabilityStatus] }
    }

    suspend fun getFreelancerStatus(freelancerId: String) {
        return dbQuery { FreelancerTable.slice(FreelancerTable.availabilityStatus).select { CoordinatorTable.employeeId eq freelancerId }.first()[FreelancerTable.availabilityStatus] }

    }

    suspend fun getFreelancers() = dbQuery {
        return@dbQuery FreelancerTable.selectAll().map { it.toSavedEmployee(FreelancerTable) }
    }


    suspend fun getFreelancerById(freelancerId: String): SavedEmployee? {
        return if (!freelancerId.startsWith("PB-FR")) null
        else dbQuery { FreelancerTable.select(FreelancerTable.employeeId eq freelancerId).first().toSavedEmployee(FreelancerTable) }
    }
}