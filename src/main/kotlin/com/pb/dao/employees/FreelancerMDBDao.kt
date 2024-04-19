package com.pb.dao.employees

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.pb.Constants.FreelancerCollectionName
import com.pb.dao.inquiries.FreelancerMDBInquiriesDao
import com.pb.models.Credentials
import com.pb.models.employee.TransactionEmployee
import com.pb.plugins.MongoDb
import kotlinx.coroutines.flow.toCollection

class FreelancerMDBDao(
    private val employeeId: String,
    database: MongoDatabase = MongoDb.mongoDb
) {

    private val selfCollection = database.getCollection<TransactionEmployee>(FreelancerCollectionName)
    val inquiriesDao = FreelancerMDBInquiriesDao(database)

    suspend fun verifyCredentials(
        credential: Credentials,
    ): Boolean {
        val filters = Filters.and(
            listOf(
                eq("employeeId", credential.employeeId), eq("password", credential.password)
            )
        )

        return buildList { selfCollection.find(filters).limit(1).toCollection(this) }.isNotEmpty()
    }

    suspend fun getSelf(): TransactionEmployee? {
        val self = mutableListOf<TransactionEmployee>()

        selfCollection.find<TransactionEmployee>(eq("employeeId", employeeId)).toCollection(self)

        if (self.isEmpty()) return null

        return self.first()
    }

    suspend fun getStatus(): Boolean? {
        return getSelf()?.availabilityStatus
    }

    suspend fun setStatus(status: Boolean): Boolean {
        val update = Updates.set("availabilityStatus", status)
        return selfCollection.updateOne(eq(employeeId), update).modifiedCount == 1L
    }
}