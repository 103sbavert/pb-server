package com.pb.dao.inquiries

import com.pb.Constants.InquiryStatusLabels.COORDINATOR_ACCEPTED
import com.pb.Constants.InquiryStatusLabels.COORDINATOR_REQUESTED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_ASSIGNED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_REQUESTED
import com.pb.Constants.InquiryStatusLabels.INQUIRY_RESOLVED
import com.pb.Constants.InquiryStatusLabels.UNASSIGNED
import com.pb.models.inquiry.ReceivedInquiry
import com.pb.models.inquiry.SavedInquiry
import com.pb.tables.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class AdminInquiriesDao(database: Database) {
    init {
        transaction(database) {
            SchemaUtils.create(UnassignedTable, CoordinatorRequestedTable, CoordinatorAcceptedTable, FreelancerRequestedTable, FreelancerAssignedTable, InquiryResolvedTable)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun getInquiryById(id: Int): SavedInquiry? {
        try {
            val inquiryResultRow = dbQuery { InquiryTable.select(InquiryTable.id eq id).first() }
            val inquiryStatusLabel = inquiryResultRow[InquiryTable.status]
            val inquiryStatus = getInquiryStatusById(inquiryStatusLabel, id)
            return inquiryResultRow.toSavedInquiry(inquiryStatus!!)
        } catch (e: java.lang.Exception) {
            return null
        }
    }

    suspend fun getInquiriesByStatus(status: String): List<SavedInquiry> {
        val inquiryStatusList = try {
            dbQuery {
                when (status) {
                    UNASSIGNED -> UnassignedTable.selectAll()
                    COORDINATOR_REQUESTED -> CoordinatorRequestedTable.selectAll()
                    COORDINATOR_ACCEPTED -> CoordinatorAcceptedTable.selectAll()
                    FREELANCER_REQUESTED -> FreelancerRequestedTable.selectAll()
                    FREELANCER_ASSIGNED -> FreelancerAssignedTable.selectAll()
                    INQUIRY_RESOLVED -> InquiryResolvedTable.selectAll()
                    else -> {
                        emptyList()
                    }
                }.map {
                    it.toInquiryStatus(status)!!
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            emptyList()
        }

        return buildList {
            inquiryStatusList.forEach { inquiryStatus ->
                dbQuery {
                    add(InquiryTable.select(InquiryTable.id eq inquiryStatus.inquiryId).first().toSavedInquiry(inquiryStatus)!!)
                }
            }
        }
    }

    suspend fun requestCoordinator(inquiryId: Int, coordinatorId: String, assignedTime: Long, countDownMillis: Long): Int = dbQuery {
        try {
            UnassignedTable.deleteWhere { UnassignedTable.inquiryId eq inquiryId }
        } catch (e: Exception) {
            e.printStackTrace()
            return@dbQuery -1
        }
        InquiryTable.update({ InquiryTable.id eq inquiryId }) { it[this.status] = COORDINATOR_REQUESTED }

        CoordinatorRequestedTable.insert {
            it[this.inquiryId] = inquiryId
            it[this.requestedCoordinator] = coordinatorId
            it[this.assignedTime] = assignedTime
            it[this.countDownMillis] = countDownMillis
        }

        return@dbQuery 1
    }

    suspend fun markInquiryAsResolved(inquiryId: Int, tags: String) = dbQuery {
        try {
            FreelancerAssignedTable.deleteWhere { FreelancerAssignedTable.inquiryId eq inquiryId }
        } catch (e: Exception) {
            e.printStackTrace()
            return@dbQuery -1
        }

        InquiryTable.update({ InquiryTable.id eq inquiryId }) { it[this.status] = INQUIRY_RESOLVED }

        InquiryResolvedTable.insert {
            it[this.inquiryId] = inquiryId
            it[this.tags] = tags
        }
    }

    suspend fun getMiscInquiries(): List<SavedInquiry> {
        return getInquiriesByStatus(COORDINATOR_REQUESTED) +
                getInquiriesByStatus(COORDINATOR_ACCEPTED) +
                getInquiriesByStatus(FREELANCER_REQUESTED) +
                getInquiriesByStatus(INQUIRY_RESOLVED)
    }

    suspend fun getUrgentInquiries(): List<SavedInquiry> {
        return getInquiriesByStatus(UNASSIGNED) + getInquiriesByStatus(FREELANCER_ASSIGNED)
    }

    private suspend fun getInquiryStatusById(status: String, id: Int) = dbQuery {
        return@dbQuery when (status) {
            UNASSIGNED -> UnassignedTable.select(UnassignedTable.inquiryId eq id)
            COORDINATOR_REQUESTED -> CoordinatorRequestedTable.select(CoordinatorRequestedTable.inquiryId eq id)
            COORDINATOR_ACCEPTED -> CoordinatorAcceptedTable.select(CoordinatorAcceptedTable.inquiryId eq id)
            FREELANCER_REQUESTED -> FreelancerRequestedTable.select(FreelancerRequestedTable.inquiryId eq id)
            FREELANCER_ASSIGNED -> FreelancerAssignedTable.select(FreelancerAssignedTable.inquiryId eq id)
            INQUIRY_RESOLVED -> InquiryResolvedTable.select(InquiryResolvedTable.inquiryId eq id)
            else -> null
        }?.firstOrNull()?.toInquiryStatus(status)
    }

    suspend fun deleteInquiry(inquiryId: Int): Int {
        try {
            dbQuery { UnassignedTable.deleteWhere { UnassignedTable.inquiryId eq inquiryId } }
            return dbQuery { InquiryTable.deleteWhere { id eq inquiryId } }
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
    }

    suspend fun createInquiry(newInquiry: ReceivedInquiry): Int {
        return try {
            dbQuery {
                val insertedId = InquiryTable.insert {
                    it[name] = newInquiry.name
                    it[description] = newInquiry.description
                    it[assignedMillis] = newInquiry.creationTime
                    it[deadlineMillis] = newInquiry.deadlineMillis
                    it[service] = newInquiry.service
                    it[contactNumber] = newInquiry.contactNumber
                    it[deliveryArea] = newInquiry.deliveryArea
                    it[reference] = newInquiry.reference
                }[InquiryTable.id]

                UnassignedTable.insert {
                    it[this.inquiryId] = insertedId
                }
                insertedId
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            -1
        }
    }
}