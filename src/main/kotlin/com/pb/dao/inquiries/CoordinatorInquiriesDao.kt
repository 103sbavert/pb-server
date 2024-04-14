package com.pb.dao.inquiries

import com.pb.Constants.InquiryStatusLabels.COORDINATOR_ACCEPTED
import com.pb.Constants.InquiryStatusLabels.COORDINATOR_REQUESTED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_ASSIGNED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_REQUESTED
import com.pb.Constants.InquiryStatusLabels.UNASSIGNED
import com.pb.models.inquiry.InquiryStatus
import com.pb.models.inquiry.SavedInquiry
import com.pb.tables.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class CoordinatorInquiriesDao(database: Database) {
    init {
        transaction(database) {
            SchemaUtils.create(CoordinatorRequestedTable, CoordinatorAcceptedTable, FreelancerRequestedTable, FreelancerAssignedTable)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun getInquiryById(id: Int): SavedInquiry? {
        try {
            val inquiryRow = dbQuery {
                InquiryTable.select {
                    InquiryTable.status eq COORDINATOR_REQUESTED or (InquiryTable.status eq COORDINATOR_ACCEPTED) or (InquiryTable.status eq FREELANCER_REQUESTED) or (InquiryTable.status eq FREELANCER_ASSIGNED) and (InquiryTable.id eq id)
                }.first()
            }
            val statusLabels = inquiryRow[InquiryTable.status]
            val inquiryStatus = getInquiryStatusById(statusLabels, id)
            return inquiryRow.toSavedInquiry(inquiryStatus!!)
        } catch (e: java.lang.Exception) {
            return null
        }
    }

    suspend fun rejectRequestedInquiry(coordinatorId: String, inquiryId: Int) = dbQuery {
        // delte from requested coordinator
        CoordinatorRequestedTable.deleteWhere { this.requestedCoordinator eq coordinatorId and (this.inquiryId eq inquiryId) }

        // add to assigned
        UnassignedTable.insert {
            this.inquiryId eq inquiryId
        }

        // update inquiry table
        InquiryTable.update({ InquiryTable.id eq inquiryId }) { it[this.status] = UNASSIGNED }
    }

    suspend fun requestFreelancer(coordinatorId: String, inquiryId: Int, freelancerId: String, assignedTime: Long, countDownMillis: Long) = dbQuery {
        if (CoordinatorRequestedTable.select { CoordinatorRequestedTable.inquiryId eq inquiryId }.firstOrNull() != null) {
            CoordinatorRequestedTable.deleteWhere { this.inquiryId eq inquiryId }
            FreelancerRequestedTable.insert {
                it[this.inquiryId] = inquiryId
                it[this.coordinator] = coordinatorId
                it[this.assignedTime] = assignedTime
                it[this.freelancerFirst] = freelancerId
                it[this.firstCountDownMillis] = countDownMillis
            }
            InquiryTable.update({ InquiryTable.id eq inquiryId }) { it[status] = FREELANCER_REQUESTED }
            1
        } else {
            val result = FreelancerRequestedTable.select { FreelancerRequestedTable.inquiryId eq inquiryId }.first().toInquiryStatus(FREELANCER_REQUESTED) as InquiryStatus.FreelancerRequested
            when {
                result.freelancerSecond == null -> FreelancerRequestedTable.update({ FreelancerRequestedTable.inquiryId eq inquiryId }) {
                    it[freelancerSecond] = freelancerId
                    it[secondCountDownMillis] = countDownMillis
                }

                result.freelancerThird == null -> FreelancerRequestedTable.update({ FreelancerRequestedTable.inquiryId eq inquiryId }) {
                    it[freelancerThird] = freelancerId
                    it[thirdCountDownMillis] = countDownMillis
                }

                else -> -1
            }
        }
    }

    suspend fun assignFreelancer(coordinatorId: String, inquiryId: Int, freelancerId: String): Int = dbQuery {
        val result = FreelancerRequestedTable.select { FreelancerRequestedTable.inquiryId eq inquiryId }.first().toInquiryStatus(FREELANCER_REQUESTED) as InquiryStatus.FreelancerRequested

        when {
            result.freelancerFirst == freelancerId && result.firstResponse == true -> FreelancerAssignedTable.insert {
                it[this.inquiryId] = inquiryId
                it[this.freelancer] = freelancerId
                it[this.coordinator] = coordinatorId
            }

            result.freelancerSecond == freelancerId && result.secondResponse == true -> FreelancerAssignedTable.insert {
                it[this.inquiryId] = inquiryId
                it[this.freelancer] = freelancerId
                it[this.coordinator] = coordinatorId
            }

            result.freelancerThird == freelancerId && result.thirdResponse == true -> FreelancerAssignedTable.insert {
                it[this.inquiryId] = inquiryId
                it[this.freelancer] = freelancerId
                it[this.coordinator] = coordinatorId
            }

            else -> return@dbQuery -1
        }
        FreelancerRequestedTable.deleteWhere { FreelancerRequestedTable.inquiryId eq inquiryId }

        return@dbQuery 1;
    }

    suspend fun getInquiriesByStatus(coordinatorId: String, status: String): List<SavedInquiry> {
        val inquiryStatuses = try {
            dbQuery {
                when (status) {
                    COORDINATOR_REQUESTED -> CoordinatorRequestedTable.select { CoordinatorRequestedTable.requestedCoordinator eq coordinatorId }
                    COORDINATOR_ACCEPTED -> CoordinatorAcceptedTable.select { CoordinatorAcceptedTable.coordinator eq coordinatorId }
                    FREELANCER_REQUESTED -> FreelancerRequestedTable.select { FreelancerRequestedTable.coordinator eq coordinatorId }
                    FREELANCER_ASSIGNED -> FreelancerAssignedTable.select { FreelancerAssignedTable.coordinator eq coordinatorId }
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
            inquiryStatuses.forEach { inquiryStatus ->
                add(dbQuery { InquiryTable.select(InquiryTable.id eq inquiryStatus.inquiryId).first().toSavedInquiry(inquiryStatus)!! })
            }
        }
    }

    suspend fun getMiscInquiries(coordinatorId: String): List<SavedInquiry> {
        return getInquiriesByStatus(coordinatorId, FREELANCER_REQUESTED) + getInquiriesByStatus(coordinatorId, FREELANCER_ASSIGNED)
    }

    suspend fun getUrgentInquiries(coordinatorId: String): List<SavedInquiry> {
        val result = getInquiriesByStatus(coordinatorId, COORDINATOR_REQUESTED) + getInquiriesByStatus(coordinatorId, COORDINATOR_ACCEPTED)

        return result + dbQuery {
            FreelancerRequestedTable.select { FreelancerRequestedTable.coordinator eq coordinatorId and (FreelancerRequestedTable.firstResponse neq null) and (FreelancerRequestedTable.secondResponse neq null) and (FreelancerRequestedTable.thirdResponse neq null) }
                .map {
                    val savedInquiry = it.toInquiryStatus(FREELANCER_REQUESTED)!!
                    InquiryTable.select { InquiryTable.id eq savedInquiry.inquiryId }.first().toSavedInquiry(savedInquiry)!!
                }
        }
    }

    private suspend fun getInquiryStatusById(status: String, id: Int) = dbQuery {
        return@dbQuery when (status) {
            COORDINATOR_REQUESTED -> CoordinatorRequestedTable.select(CoordinatorRequestedTable.inquiryId eq id)
            COORDINATOR_ACCEPTED -> CoordinatorAcceptedTable.select { CoordinatorAcceptedTable.inquiryId eq id }
            FREELANCER_REQUESTED -> FreelancerRequestedTable.select(FreelancerRequestedTable.inquiryId eq id)
            FREELANCER_ASSIGNED -> FreelancerAssignedTable.select(FreelancerAssignedTable.inquiryId eq id)
            else -> null
        }?.firstOrNull()?.toInquiryStatus(status)
    }
}