package com.pb.dao.inquiries

import com.pb.Constants.InquiryStatusLabels.COORDINATOR_ACCEPTED
import com.pb.Constants.InquiryStatusLabels.COORDINATOR_REQUESTED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_ASSIGNED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_REQUESTED
import com.pb.Constants.InquiryStatusLabels.INQUIRY_RESOLVED
import com.pb.Constants.InquiryStatusLabels.UNASSIGNED
import com.pb.models.inquiry.InquiryStatus
import com.pb.models.inquiry.SavedInquiry
import com.pb.tables.*
import org.jetbrains.exposed.sql.ResultRow

internal fun ResultRow.toInquiryStatus(status: String): InquiryStatus? {
    return when (status) {
        UNASSIGNED -> InquiryStatus.Unassigned(get(UnassignedTable.inquiryId))
        COORDINATOR_REQUESTED -> InquiryStatus.CoordinatorRequested(
            get(CoordinatorRequestedTable.inquiryId),
            get(CoordinatorRequestedTable.requestedCoordinator),
            get(CoordinatorRequestedTable.assignedTime),
            get(CoordinatorRequestedTable.countDownMillis)
        )

        COORDINATOR_ACCEPTED -> InquiryStatus.CoordinatorAccepted(
            get(CoordinatorAcceptedTable.inquiryId),
            get(CoordinatorAcceptedTable.coordinator)
        )

        FREELANCER_REQUESTED -> InquiryStatus.FreelancerRequested(
            get(FreelancerRequestedTable.inquiryId),
            get(FreelancerRequestedTable.coordinator),
            get(FreelancerRequestedTable.freelancerFirst),
            get(FreelancerRequestedTable.freelancerSecond),
            get(FreelancerRequestedTable.freelancerThird),
            get(FreelancerRequestedTable.assignedTime),
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

internal fun ResultRow.toSavedInquiry(status: InquiryStatus): SavedInquiry? {
    return try {
        with(InquiryTable) {
            SavedInquiry(get(id), get(name), get(description), get(assignedMillis), get(deadlineMillis), get(service), get(contactNumber), get(deliveryArea), get(reference), status)
        }
    } catch (e: Exception) {
        null
    }
}