package com.pb.dao

import com.pb.Constants.InquiryStatusLabels.COORDINATOR_REQUESTED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_ASSIGNED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_REQUESTED
import com.pb.Constants.InquiryStatusLabels.INQUIRY_RESOLVED
import com.pb.Constants.InquiryStatusLabels.UNASSIGNED
import com.pb.models.inquiry.InquiryStatus
import com.pb.models.inquiry.InquiryUpdateAction
import com.pb.models.inquiry.ReceivedInquiry
import com.pb.models.inquiry.SavedInquiry
import com.pb.tables.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction


class InquiryTableDao(database: Database) {

    init {
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(
                InquiryTable, UnassignedTable, CoordinatorRequestedTable, CoordinatorAcceptedTable, FreelancerRequestedTable, FreelancerAssignedTable, InquiryResolvedTable
            )
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun getInquiryById(id: Int) = dbQuery {
        try {
            val inquiryResultRow = InquiryTable.select(InquiryTable.id eq id).first()
            val inquiryStatus = inquiryResultRow[InquiryTable.status]
            val inquiryStatusObj = getInquiryStatusById(inquiryStatus, id)
            val savedInquiryObj = inquiryResultRow.toSavedInquiry(inquiryStatusObj!!)
            return@dbQuery savedInquiryObj
        } catch (e: java.lang.Exception) {
            return@dbQuery null
        }
    }

    suspend fun getInquiriesByStatus(status: String) = dbQuery {
        val inquiryStatusList = try {
            when (status) {
                UNASSIGNED -> UnassignedTable.selectAll()
                COORDINATOR_REQUESTED -> CoordinatorRequestedTable.selectAll()
                FREELANCER_REQUESTED -> FreelancerRequestedTable.selectAll()
                FREELANCER_ASSIGNED -> FreelancerAssignedTable.selectAll()
                INQUIRY_RESOLVED -> InquiryResolvedTable.selectAll()
                else -> {
                    emptyList()
                }
            }.map {
                it.toInquiryStatus(status)!!
            }
        } catch (e: java.lang.Exception) {
            println("stack trace beginning")
            e.printStackTrace()
            emptyList()
        }

        return@dbQuery buildList {
            inquiryStatusList.forEach { inquiryStatus ->
                add(InquiryTable.select(InquiryTable.id eq inquiryStatus.inquiryId).first().toSavedInquiry(inquiryStatus)!!)
            }
        }
    }

    private suspend fun getInquiryStatusById(status: String, id: Int) = dbQuery {
        return@dbQuery when (status) {
            UNASSIGNED -> UnassignedTable.select(UnassignedTable.inquiryId eq id)
            COORDINATOR_REQUESTED -> CoordinatorRequestedTable.select(CoordinatorRequestedTable.inquiryId eq id)
            FREELANCER_REQUESTED -> FreelancerRequestedTable.select(FreelancerRequestedTable.inquiryId eq id)
            FREELANCER_ASSIGNED -> FreelancerAssignedTable.select(FreelancerAssignedTable.inquiryId eq id)
            INQUIRY_RESOLVED -> InquiryResolvedTable.select(InquiryResolvedTable.inquiryId eq id)
            else -> null
        }?.firstOrNull()?.toInquiryStatus(status)
    }

    suspend fun getInquiryByRequestedCoordinator(coordinatorId: String): List<SavedInquiry> = dbQuery {
        return@dbQuery try {
            val inquiryStatus = COORDINATOR_REQUESTED
            val inquiryStatusObjList = CoordinatorRequestedTable.select(CoordinatorRequestedTable.requestedCoordinator eq coordinatorId).map { it.toInquiryStatus(inquiryStatus) }
            buildList {
                for (it in inquiryStatusObjList) {
                    InquiryTable.select(InquiryTable.id eq it!!.inquiryId).first().toSavedInquiry(it)?.let { it1 -> add(it1) }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            emptyList()
        }
    }


    suspend fun getInquiryByAssignedCoordinator(coordinatorId: String): List<SavedInquiry>? = dbQuery {
        return@dbQuery try {
            val inquiryStatus1 = FREELANCER_REQUESTED
            val inquiryStatus2 = FREELANCER_REQUESTED
            val inquiryStatusObjList1 = FreelancerRequestedTable.select(FreelancerRequestedTable.coordinator eq coordinatorId).map { it.toInquiryStatus(inquiryStatus1) }
            val inquiryStatusObjList3 = FreelancerAssignedTable.select(FreelancerAssignedTable.coordinator eq coordinatorId).map { it.toInquiryStatus(inquiryStatus2) }
            buildList {
                for (inquiryStatus in inquiryStatusObjList1) {
                    InquiryTable.select(InquiryTable.id eq inquiryStatus!!.inquiryId).first().toSavedInquiry(inquiryStatus)?.let { it1 -> add(it1) }
                }
                for (inquiryStatus in inquiryStatusObjList3) {
                    InquiryTable.select(InquiryTable.id eq inquiryStatus!!.inquiryId).first().toSavedInquiry(inquiryStatus)?.let { it1 -> add(it1) }
                }
            }
        } catch (e: java.lang.Exception) {
            null
        }
    }

    suspend fun getInquiryByRequestedFreelancer(freelancerId: String): List<SavedInquiry> = dbQuery {
        return@dbQuery try {
            val inquiryStatus = "FreelancerRequested"
            val inquiryStatusObjList = FreelancerRequestedTable.select(
                (FreelancerRequestedTable.freelancerFirst eq freelancerId and (FreelancerRequestedTable.firstResponse eq null)) or (FreelancerRequestedTable.freelancerSecond eq freelancerId and (FreelancerRequestedTable.secondResponse eq null)) or (FreelancerRequestedTable.freelancerThird eq freelancerId and (FreelancerRequestedTable.thirdResponse eq null))
            ).map { it.toInquiryStatus(inquiryStatus) }
            buildList {
                for (it in inquiryStatusObjList) {
                    InquiryTable.select(InquiryTable.id eq it!!.inquiryId).first().toSavedInquiry(it)?.let { it1 -> add(it1) }
                }
            }
        } catch (e: java.lang.Exception) {
            emptyList()
        }
    }

    suspend fun selectByAssignedFreelancer(freelancerId: String): List<SavedInquiry>? = dbQuery {
        return@dbQuery try {
            val inquiryStatus = "FreelancerAssigned"
            val inquiryStatusObjList = FreelancerAssignedTable.select(FreelancerAssignedTable.freelancer eq freelancerId).map { it.toInquiryStatus(inquiryStatus) }
            buildList {
                for (it in inquiryStatusObjList) {
                    InquiryTable.select(InquiryTable.id eq it!!.inquiryId).first().toSavedInquiry(it)?.let { it1 -> add(it1) }
                }
            }
        } catch (e: java.lang.Exception) {
            null
        }
    }

    suspend fun createNewInquiryAsAdmin(newInquiry: ReceivedInquiry) = dbQuery {
        return@dbQuery try {
            val inquiryId = InquiryTable.insert {
                it[name] = newInquiry.name
                it[description] = newInquiry.description
                it[assignedMillis] = newInquiry.assigningMillis
                it[deadlineMillis] = newInquiry.deadlineMillis
                it[service] = newInquiry.service
                it[contactNumber] = newInquiry.contactNumber
                it[deliveryArea] = newInquiry.deliveryArea
                it[reference] = newInquiry.reference
            }[InquiryTable.id]
            UnassignedTable.insert {
                it[this.inquiryId] = inquiryId
            }
            inquiryId
        } catch (e: java.lang.Exception) {
            -1
        }
    }

    suspend fun updateInquiry(inquiryUpdateAction: InquiryUpdateAction) {
        try {
            val row = dbQuery { InquiryTable.select(InquiryTable.id eq inquiryUpdateAction.inquiryId).first() }
            val inquiryId = row[InquiryTable.id]

            when (inquiryUpdateAction) {
                is InquiryUpdateAction.DeleteInquiryAsAdmin -> deleteInquiryAsAdmin(inquiryId)
                is InquiryUpdateAction.RejectInquiryAsCoordinator -> rejectInquiryAsCoordinator(inquiryId)
                is InquiryUpdateAction.RejectInquiryAsFreelancer -> rejectInquiryAsFreelancer(inquiryId, inquiryUpdateAction.rejectingFreelancerId)
                is InquiryUpdateAction.AcceptInquiryAsFreelancer -> acceptInquiryAsFreelancer(inquiryId, inquiryUpdateAction.acceptorFreelancerId)
                is InquiryUpdateAction.AssignFreelancerAsCoordinator -> assignFreelancerAsCoordinator(inquiryId, inquiryUpdateAction.assignorCoordinatorId, inquiryUpdateAction.assignedFreelancerId)
                is InquiryUpdateAction.CreateInquiryAsAdmin -> createNewInquiryAsAdmin(inquiryUpdateAction.inquiry)
                is InquiryUpdateAction.MarkResolvedAsAdmin -> markResolvedAsAdmin(inquiryId)
                is InquiryUpdateAction.RequestCoordinatorAsAdmin -> { requestCoordinatorAsAdmin(inquiryId, inquiryUpdateAction.requestedCoordinatorId, inquiryUpdateAction.countDownMillis) }
                is InquiryUpdateAction.RequestFreelancerAsCoordinator -> requestFreelancerAsCoordinator(inquiryId, inquiryUpdateAction.requestingCoordinatorId, inquiryUpdateAction.requestedFreelancerId, inquiryUpdateAction.countDownMillis)
                else -> {}
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            -1
        }
    }

    private suspend fun requestFreelancerAsCoordinator(inquiryId: Int, coordinatorId: String, requestedFreelancerId: String, countDownMillis: Long): Int = dbQuery {
        CoordinatorAcceptedTable.deleteWhere { this.inquiryId eq inquiryId }
        val tempRow = FreelancerRequestedTable.select { FreelancerRequestedTable.inquiryId eq inquiryId }.firstOrNull()
        return@dbQuery if (tempRow != null) {
            val inquiryStatusObj = tempRow.toInquiryStatus(FREELANCER_REQUESTED) as InquiryStatus.FreelancerRequested
            when (null) {
                inquiryStatusObj.freelancerFirst -> {
                    FreelancerRequestedTable.update({ FreelancerRequestedTable.inquiryId eq inquiryId }) {
                        it[this.freelancerFirst] = requestedFreelancerId
                        it[this.firstCountDownMillis] = countDownMillis
                    }
                }

                inquiryStatusObj.freelancerSecond -> {
                    FreelancerRequestedTable.update({ FreelancerRequestedTable.inquiryId eq inquiryId }) {
                        it[this.freelancerSecond] = requestedFreelancerId
                        it[this.secondCountDownMillis] = countDownMillis
                    }
                }

                inquiryStatusObj.freelancerThird -> {
                    FreelancerRequestedTable.update({ FreelancerRequestedTable.inquiryId eq inquiryId }) {
                        it[this.freelancerThird] = requestedFreelancerId
                        it[this.thirdCountDownMillis] = countDownMillis
                    }
                }

                else -> {
                    throw IllegalStateException("Cannot request more than three freelancers")
                }
            }
        } else {
            FreelancerRequestedTable.insert {
                it[this.inquiryId] = inquiryId
                it[this.freelancerFirst] = requestedFreelancerId
                it[this.firstCountDownMillis] = countDownMillis
                it[this.coordinator] = coordinatorId
            }[FreelancerRequestedTable.inquiryId]
        }
    }

    private suspend fun requestCoordinatorAsAdmin(inquiryId: Int, requestedCoordinatorId: String, countDownMillis: Long): Int = dbQuery {
        UnassignedTable.deleteWhere { UnassignedTable.inquiryId eq inquiryId }
        InquiryTable.update({ InquiryTable.id eq inquiryId }) { it[this.status] = COORDINATOR_REQUESTED }
        return@dbQuery CoordinatorRequestedTable.insert {
            it[this.inquiryId] = inquiryId
            it[this.requestedCoordinator] = requestedCoordinatorId
            it[this.countDownMillis] = countDownMillis
        }[CoordinatorRequestedTable.inquiryId]
    }

    private suspend fun markResolvedAsAdmin(inquiryId: Int): Int = dbQuery {
        FreelancerAssignedTable.deleteWhere { this.inquiryId eq inquiryId }
        return@dbQuery InquiryResolvedTable.insert {
            it[this.inquiryId] = inquiryId
        }[InquiryResolvedTable.inquiryId]
    }

    private suspend fun assignFreelancerAsCoordinator(inquiryId: Int, assignorCoordinator: String, assignedFreelancer: String): Int = dbQuery {
        CoordinatorRequestedTable.deleteWhere { this.inquiryId eq inquiryId }
        InquiryTable.update({ InquiryTable.id eq inquiryId }) { it[status] = FREELANCER_ASSIGNED }
        return@dbQuery FreelancerAssignedTable.insert {
            it[this.inquiryId] = inquiryId
            it[this.coordinator] = assignorCoordinator
            it[this.freelancer] = assignedFreelancer
        }[FreelancerAssignedTable.inquiryId]
    }

    private suspend fun acceptInquiryAsFreelancer(inquiryId: Int, acceptorFreelancerId: String): Int = dbQuery {
        println("Inquiry log")
        val inquiryStatusObj = FreelancerRequestedTable.select(FreelancerRequestedTable.inquiryId eq inquiryId).first().toInquiryStatus(FREELANCER_REQUESTED) as InquiryStatus.FreelancerRequested
        println("Inquiry log freelancer")
        return@dbQuery when {
            inquiryStatusObj.freelancerFirst == acceptorFreelancerId -> {
                FreelancerRequestedTable.update({ FreelancerRequestedTable.inquiryId eq inquiryId }) { updateStatement -> updateStatement[firstResponse] = true }
            }

            inquiryStatusObj.freelancerSecond == acceptorFreelancerId -> {
                FreelancerRequestedTable.update({ FreelancerRequestedTable.inquiryId eq inquiryId }) { updateStatement -> updateStatement[secondResponse] = true }
            }

            inquiryStatusObj.freelancerThird == acceptorFreelancerId -> {
                FreelancerRequestedTable.update({ FreelancerRequestedTable.inquiryId eq inquiryId }) { updateStatement -> updateStatement[thirdResponse] = true }
            }

            else -> {
                throw IllegalStateException("Freelancer was never requested this inquiry")
            }
        }
    }

    private suspend fun rejectInquiryAsFreelancer(inquiryId: Int, rejectingFreelancerId: String): Int = dbQuery {
        val inquiryStatusObj = FreelancerRequestedTable.select(FreelancerRequestedTable.inquiryId eq inquiryId).first().toInquiryStatus(FREELANCER_REQUESTED) as InquiryStatus.FreelancerRequested

        return@dbQuery when {
            inquiryStatusObj.freelancerFirst == rejectingFreelancerId -> {
                FreelancerRequestedTable.update({ FreelancerRequestedTable.inquiryId eq inquiryId }) { updateStatement -> updateStatement[firstResponse] = false }
            }

            inquiryStatusObj.freelancerSecond == rejectingFreelancerId -> {
                FreelancerRequestedTable.update({ FreelancerRequestedTable.inquiryId eq inquiryId }) { updateStatement -> updateStatement[secondResponse] = false }
            }

            inquiryStatusObj.freelancerThird == rejectingFreelancerId -> {
                FreelancerRequestedTable.update({ FreelancerRequestedTable.inquiryId eq inquiryId }) { updateStatement -> updateStatement[thirdResponse] = false }
            }

            else -> {
                throw IllegalStateException("Freelancer $rejectingFreelancerId was never requested this inquiry ($inquiryId)")
            }
        }
    }

    private suspend fun rejectInquiryAsCoordinator(inquiryId: Int): InsertStatement<Number> = dbQuery {
        CoordinatorRequestedTable.deleteWhere { CoordinatorRequestedTable.inquiryId eq inquiryId }
        InquiryTable.update { updateStatement -> updateStatement[this.status] = UNASSIGNED }
        return@dbQuery UnassignedTable.insert { insertStatement -> insertStatement[this.inquiryId] = inquiryId }
    }

    private suspend fun deleteInquiryAsAdmin(inquiryId: Int): Int = dbQuery {
        UnassignedTable.deleteWhere { UnassignedTable.inquiryId eq inquiryId }
        return@dbQuery InquiryTable.deleteWhere { id eq inquiryId }
    }

    private fun ResultRow.toSavedInquiry(status: InquiryStatus): SavedInquiry? {
        return try {
            with(InquiryTable) {
                SavedInquiry(get(id), get(name), get(description), get(assignedMillis), get(deadlineMillis), get(service), get(contactNumber), get(deliveryArea), get(reference), status)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun ResultRow.toInquiryStatus(status: String): InquiryStatus? {
        System.err.print(status);
        return when (status) {

            UNASSIGNED -> InquiryStatus.Unassigned(get(UnassignedTable.inquiryId))
            COORDINATOR_REQUESTED -> InquiryStatus.CoordinatorRequested(get(CoordinatorRequestedTable.inquiryId), get(CoordinatorRequestedTable.requestedCoordinator), get(CoordinatorRequestedTable.countDownMillis))
            FREELANCER_REQUESTED -> InquiryStatus.FreelancerRequested(
                get(FreelancerRequestedTable.inquiryId),
                get(FreelancerRequestedTable.coordinator),
                get(FreelancerRequestedTable.freelancerFirst),
                get(FreelancerRequestedTable.freelancerSecond),
                get(FreelancerRequestedTable.freelancerThird),
                get(FreelancerRequestedTable.firstCountDownMillis),
                get(FreelancerRequestedTable.secondCountDownMillis),
                get(FreelancerRequestedTable.thirdCountDownMillis),
                get(FreelancerRequestedTable.firstResponse),
                get(FreelancerRequestedTable.secondResponse),
                get(FreelancerRequestedTable.thirdResponse),
            )

            FREELANCER_ASSIGNED -> {
                val coordinator = get(FreelancerAssignedTable.coordinator)
                val freelancer = get(FreelancerAssignedTable.freelancer)

                InquiryStatus.FreelancerAssigned(get(FreelancerAssignedTable.inquiryId), coordinator, freelancer)
            }

            INQUIRY_RESOLVED -> InquiryStatus.InquiryResolved(get(InquiryResolvedTable.inquiryId), get(InquiryResolvedTable.tags))
            else -> null
        }
    }

}
