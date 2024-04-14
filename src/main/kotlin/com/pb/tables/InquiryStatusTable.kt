package com.pb.tables

import org.jetbrains.exposed.sql.Table

sealed class InquiryStatusTable(name: String) : Table(name) {
    private val id = integer("id").autoIncrement()
    val inquiryId = reference("inquiry_id", InquiryTable.id).uniqueIndex()
    override val primaryKey = PrimaryKey(id)
}

object UnassignedTable : InquiryStatusTable("Unassigned")

object CoordinatorRequestedTable : InquiryStatusTable("CoordinatorRequested") {
    val requestedCoordinator = reference("requested_coordinator", CoordinatorTable.employeeId)
    val assignedTime = long("assigned_time")
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
    val assignedTime = long("assigned_time")
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