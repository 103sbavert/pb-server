package com.pb.tables

import org.jetbrains.exposed.sql.Table

sealed class InquiryStatusTable(name: String) : Table(name) {
    private val id = integer("id").autoIncrement()
    val inquiryId = reference("inquiry_id", InquiryTable.id).uniqueIndex()
    override val primaryKey = PrimaryKey(id)

    /*    fun selectByInquiryId(inquiryId: Int): InquiryStatus? {
            val row = select(this@InquiryStatusTable.inquiryId eq inquiryId).singleOrNull() ?: return null
            return row.toInquiryStatus()
        }

        fun selectAllInquiries(): List<InquiryStatus> {
            return selectAll().map { it.toInquiryStatus() }
        }

        internal fun ResultRow.toInquiryStatus(): InquiryStatus {
            return when (this[label]) {
                InquiryStatus.UNASSIGNED -> InquiryStatus.Unassigned(inquiryId = this[inquiryId])
                InquiryStatus.COORDINATOR_REQUESTED -> InquiryStatus.CoordinatorRequested(inquiryId = this[inquiryId], requestedCoordinator = this[requestedCoordinator], countDownMillis = this[countDownMillis])
                InquiryStatus.FREELANCER_REQUESTED -> InquiryStatus.FreelancerRequested(
                    inquiryId = this[inquiryId],
                    coordinator = this[coordinator],
                    freelancerFirst = this[freelancerFirst],
                    freelancerSecond = this[freelancerSecond],
                    freelancerThird = this[freelancerThird],
                    firstCountDownMillis = this[firstCountDownMillis],
                    secondCountDownMillis = this[secondCountDownMillis],
                    thirdCountDownMillis = this[thirdCountDownMillis]
                )

                InquiryStatus.FREELANCER_ACCEPTED -> InquiryStatus.FreelancerAccepted(
                    inquiryId = this[inquiryId],
                    coordinator = this[FreelancerAcceptedTable.coordinator],
                    freelancerFirst = this[FreelancerAcceptedTable.freelancerFirst],
                    freelancerSecond = this[FreelancerAcceptedTable.freelancerSecond],
                    freelancerThird = this[FreelancerAcceptedTable.freelancerThird]
                )

                InquiryStatus.FREELANCER_ASSIGNED -> InquiryStatus.FreelancerAssigned(
                    inquiryId = this[inquiryId], coordinator = this[FreelancerAssignedTable.coordinator], freelancer = this[FreelancerAssignedTable.freelancer]
                )

                InquiryStatus.INQUIRY_RESOLVED -> InquiryStatus.InquiryResolved(inquiryId = this[inquiryId])
            }
        }*/
}

object UnassignedTable : InquiryStatusTable("Unassigned")

object CoordinatorRequestedTable : InquiryStatusTable("CoordinatorRequested") {
    val requestedCoordinator = reference("requested_coordinator", CoordinatorTable.employeeId)
    val countDownMillis = long("count_down_millis")
}


object CoordinatorAcceptedTable : InquiryStatusTable("CoordinatorAccepted") {
    val coordinator = reference("coordinator", CoordinatorTable.employeeId)
}

object FreelancerRequestedTable : InquiryStatusTable("FreelancerRequested") {
    val coordinator = reference("assigned_coordinator", CoordinatorTable.employeeId)
    val freelancerFirst = reference("first_freelancer", FreelancerTable.employeeId).nullable()
    val freelancerSecond = reference("second_freelancer", FreelancerTable.employeeId).nullable()
    val freelancerThird = reference("third_freelancer", FreelancerTable.employeeId).nullable()
    val firstCountDownMillis = long("first_count_down").nullable()
    val secondCountDownMillis = long("second_count_down").nullable()
    val thirdCountDownMillis = long("third_count_down").nullable()
    val firstResponse = bool("first_response").nullable().default(null)
    val secondResponse = bool("second_response").nullable().default(null)
    val thirdResponse = bool("third_response").nullable().default(null)
}

object FreelancerAssignedTable : InquiryStatusTable("FreelancerAssigned") {
    val coordinator = reference("coordinator", CoordinatorTable.employeeId)
    val freelancer = reference("freelancer", FreelancerTable.employeeId)
}

object InquiryResolvedTable : InquiryStatusTable("InquiryResolved") {
    val tags = text("tags").default("")

}