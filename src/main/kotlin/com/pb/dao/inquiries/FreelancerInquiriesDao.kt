package com.pb.dao.inquiries

import com.pb.Constants.InquiryStatusLabels.FREELANCER_ASSIGNED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_REQUESTED
import com.pb.models.inquiry.InquiryStatus
import com.pb.models.inquiry.SavedInquiry
import com.pb.tables.FreelancerAssignedTable
import com.pb.tables.FreelancerRequestedTable
import com.pb.tables.InquiryTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class FreelancerInquiriesDao(database: Database) {
    init {
        transaction(database) {
            SchemaUtils.create(FreelancerRequestedTable, FreelancerAssignedTable)
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun getInquiryById(id: Int): SavedInquiry? {
        try {
            val inquiryRow = dbQuery {
                InquiryTable.select {
                    InquiryTable.status eq FREELANCER_REQUESTED or (InquiryTable.status eq FREELANCER_ASSIGNED) and (InquiryTable.id eq id)
                }.first()
            }
            val statusLabels = inquiryRow[InquiryTable.status]
            val inquiryStatus = getInquiryStatusById(statusLabels, id)
            return inquiryRow.toSavedInquiry(inquiryStatus!!)
        } catch (e: java.lang.Exception) {
            return null
        }
    }

    suspend fun getInquiriesByStatus(selfId: String, status: String): List<SavedInquiry> {
        val inquiryStatuses = try {
            dbQuery {
                when (status) {
                    FREELANCER_REQUESTED -> {
                        val tempStatuses = FreelancerRequestedTable.select {
                            FreelancerRequestedTable.freelancerFirst eq selfId or (FreelancerRequestedTable.freelancerSecond eq selfId) or (FreelancerRequestedTable.freelancerThird eq selfId)
                        }.map { it.toInquiryStatus(FREELANCER_REQUESTED) as InquiryStatus.FreelancerRequested }.filter {
                            when (selfId) {
                                it.freelancerFirst -> it.firstResponse == null
                                it.freelancerSecond -> it.secondResponse == null
                                it.freelancerThird -> it.thirdResponse == null
                                else -> throw UnknownError("pls come back here")
                            }
                        }

                        tempStatuses
                    }

                    FREELANCER_ASSIGNED -> FreelancerAssignedTable.select { FreelancerAssignedTable.freelancer eq selfId }.map { it.toInquiryStatus(FREELANCER_ASSIGNED) }
                    else -> {
                        emptyList()
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            emptyList()
        }

        return buildList {
            inquiryStatuses.forEach { inquiryStatus ->
                add(dbQuery { InquiryTable.select(InquiryTable.id eq inquiryStatus!!.inquiryId).first().toSavedInquiry(inquiryStatus)!! })
            }
        }
    }

    suspend fun getMiscInquiries(selfId: String): List<SavedInquiry> {
        return getInquiriesByStatus(selfId, FREELANCER_ASSIGNED)
    }

    suspend fun getUrgentInquiries(selfId: String): List<SavedInquiry> {
        return getInquiriesByStatus(selfId, FREELANCER_REQUESTED)
    }

    suspend fun acceptInquiry(freelancerId: String, inquiryId: Int) = dbQuery {
        val inquiryStatus = runCatching { FreelancerRequestedTable.select { FreelancerRequestedTable.inquiryId eq inquiryId }.first().toInquiryStatus(FREELANCER_REQUESTED) as InquiryStatus.FreelancerRequested }.getOrElse {
            return@dbQuery -1
        }

        when (freelancerId) {
            inquiryStatus.freelancerFirst -> FreelancerRequestedTable.update({ FreelancerRequestedTable.inquiryId eq inquiryId }) { it[firstResponse] = true }
            inquiryStatus.freelancerSecond -> FreelancerRequestedTable.update({ FreelancerRequestedTable.inquiryId eq inquiryId }) { it[secondResponse] = true }
            inquiryStatus.freelancerThird -> FreelancerRequestedTable.update({ FreelancerRequestedTable.inquiryId eq inquiryId }) { it[thirdResponse] = true }
        }

        return@dbQuery 1
    }

    suspend fun rejectInquiry(freelancerId: String, inquiryId: Int) = dbQuery {
        val inquiryStatus = runCatching { FreelancerRequestedTable.select { FreelancerRequestedTable.inquiryId eq inquiryId }.first().toInquiryStatus(FREELANCER_REQUESTED) as InquiryStatus.FreelancerRequested }.getOrElse {
            it.printStackTrace()
            return@dbQuery -1
        }

        when (freelancerId) {
            inquiryStatus.freelancerFirst -> FreelancerRequestedTable.update({ FreelancerRequestedTable.inquiryId eq inquiryId }) { it[firstResponse] = false }
            inquiryStatus.freelancerSecond -> FreelancerRequestedTable.update({ FreelancerRequestedTable.inquiryId eq inquiryId }) { it[secondResponse] = false }
            inquiryStatus.freelancerThird -> FreelancerRequestedTable.update({ FreelancerRequestedTable.inquiryId eq inquiryId }) { it[thirdResponse] = false }
        }
//
//        val temp = FreelancerRequestedTable.select { FreelancerRequestedTable.inquiryId eq inquiryId }.first().toInquiryStatus(FREELANCER_REQUESTED) as InquiryStatus.FreelancerRequested
//        if (temp.firstResponse != null && temp.secondResponse != null && temp.thirdResponse != null) {
//            FreelancerRequestedTable.deleteWhere { FreelancerRequestedTable.inquiryId eq inquiryId }
//            InquiryTable.update({ InquiryTable.id eq inquiryId }) { it[id] =  }
//        }

        return@dbQuery 1
    }


    private suspend fun getInquiryStatusById(status: String, id: Int) = dbQuery {
        return@dbQuery when (status) {
            FREELANCER_REQUESTED -> FreelancerRequestedTable.select(FreelancerRequestedTable.inquiryId eq id)
            FREELANCER_ASSIGNED -> FreelancerAssignedTable.select(FreelancerAssignedTable.inquiryId eq id)
            else -> null
        }?.firstOrNull()?.toInquiryStatus(status)
    }
}