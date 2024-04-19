package com.pb.dao.employees

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.pb.Constants
import com.pb.Constants.AdminCollectionName
import com.pb.Constants.CoordinatorCollectionName
import com.pb.Constants.FreelancerCollectionName
import com.pb.Constants.defaultEmployeePassword
import com.pb.dao.inquiries.AdminMDBInquiriesDao
import com.pb.enums.EmployeeRole
import com.pb.enums.EmployeeRole.Companion.parseEmployeeId
import com.pb.models.Credentials
import com.pb.models.employee.SavedEmployee
import com.pb.models.employee.TransactionEmployee
import com.pb.plugins.MongoDb
import kotlinx.coroutines.flow.toCollection

class AdminMDBDao(
    private val employeeId: String,
    database: MongoDatabase = MongoDb.mongoDb
) {

    private val selfCollection = database.getCollection<SavedEmployee>(AdminCollectionName)
    private val coordinatorCollection = database.getCollection<SavedEmployee>(CoordinatorCollectionName)
    private val freelancerCollection = database.getCollection<SavedEmployee>(FreelancerCollectionName)
    val inquiriesDao = AdminMDBInquiriesDao(database)


    suspend fun verifyCredentials(
        credential: Credentials,
    ): Boolean {
        val filters = Filters.and(
            eq("employeeId", credential.employeeId),
            eq("password", credential.password)
        )

        return buildList { selfCollection.find(filters).limit(1).toCollection(this) }.isNotEmpty()
    }

    suspend fun createEmployee(transactionEmployee: TransactionEmployee): Boolean {
        if (!transactionEmployee.employeeId.matches(Constants.employeeIdRegex)) {
            System.err.println("employee id ${transactionEmployee.employeeId}")
            return false
        }

        val roleSpecificString  = when (transactionEmployee.role) {
            EmployeeRole.ADMIN -> "PB-AM-"
            EmployeeRole.COORDINATOR -> "PB-PC-"
            EmployeeRole.FREELANCER -> "PB-FR-"
        }

        val savedEmployee = SavedEmployee(
            roleSpecificString + transactionEmployee.employeeId,
            transactionEmployee.name,
            transactionEmployee.emailAddress,
            defaultEmployeePassword,
            transactionEmployee.contactNumber,
            transactionEmployee.availabilityStatus,
            transactionEmployee.role
        )

        val insertedId = when (transactionEmployee.role) {
            EmployeeRole.ADMIN -> selfCollection.insertOne(savedEmployee).insertedId
            EmployeeRole.COORDINATOR -> coordinatorCollection.insertOne(savedEmployee).insertedId
            EmployeeRole.FREELANCER -> freelancerCollection.insertOne(savedEmployee).insertedId
        }

        System.err.println(transactionEmployee)

        return insertedId != null
    }

    suspend fun getSelf(): TransactionEmployee? {
        val self = mutableListOf<TransactionEmployee>()

        selfCollection.find<TransactionEmployee>(eq("employeeId", employeeId)).toCollection(self)

        if (self.isEmpty()) return null

        return self.first()
    }

    suspend fun getCoordinatorById(employeeId: String): TransactionEmployee? {
        val filter = eq("employeeId", employeeId)
        val coordinator = mutableListOf<TransactionEmployee>()

        coordinatorCollection.find<TransactionEmployee>(filter).toCollection(coordinator)

        if (coordinator.isEmpty()) return null

        return coordinator.first()
    }

    suspend fun getFreelancerById(employeeId: String): TransactionEmployee? {
        val filter = eq("employeeId", employeeId)
        val freelancer = mutableListOf<TransactionEmployee>()

        freelancerCollection.find<TransactionEmployee>(filter).toCollection(freelancer)

        if (freelancer.isEmpty()) return null

        return freelancer.first()
    }

    suspend fun getEmployeesByRole(role: EmployeeRole): MutableList<TransactionEmployee> {
        val employees = mutableListOf<TransactionEmployee>()

        when (role) {
            EmployeeRole.ADMIN -> selfCollection.find<TransactionEmployee>().toCollection(employees)
            EmployeeRole.COORDINATOR -> coordinatorCollection.find<TransactionEmployee>().toCollection(employees)
            EmployeeRole.FREELANCER -> freelancerCollection.find<TransactionEmployee>().toCollection(employees)
        }

        return employees
    }

    suspend fun removeEmployee(employeeId: String): Boolean {
        val role = employeeId.parseEmployeeId()

        return when (role) {
            EmployeeRole.ADMIN -> throw IllegalStateException("Cannot delete an admin as an admin")
            EmployeeRole.COORDINATOR -> coordinatorCollection.deleteOne(eq("employeeId", employeeId)).deletedCount > 0
            EmployeeRole.FREELANCER -> freelancerCollection.deleteOne(eq("employeeId", employeeId)).deletedCount > 0
        }
    }

    suspend fun removeFreelancerId(employeeId: String): Boolean {
        return freelancerCollection.deleteOne(eq("employeeId", employeeId)).deletedCount > 0
    }

    suspend fun removeCoordinatorById(employeeId: String): Boolean {
        return coordinatorCollection.deleteOne(eq("employeeId", employeeId)).deletedCount > 0
    }
}