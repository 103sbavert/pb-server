package com.pb.models.inquiry

import com.pb.Constants.InquiryStatusLabels.COORDINATOR_ACCEPTED
import com.pb.Constants.InquiryStatusLabels.COORDINATOR_REQUESTED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_ASSIGNED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_REQUESTED
import com.pb.Constants.InquiryStatusLabels.INQUIRY_RESOLVED
import com.pb.Constants.InquiryStatusLabels.UNASSIGNED
import com.pb.serializer.InquiryStatusBsonSerializer
import kotlinx.serialization.Serializable

@Serializable(InquiryStatusBsonSerializer::class)
sealed class InquiryStatus {
    abstract val label: String

    @Serializable
    class Unassigned() : InquiryStatus() {
        override val label: String = UNASSIGNED
    }

    @Serializable
    class CoordinatorRequested(
        val coordinatorRequest: EmployeeRequest.CoordinatorRequest
    ) : InquiryStatus() {
        override val label: String = COORDINATOR_REQUESTED
    }

    @Serializable
    class CoordinatorAccepted(
        val coordinatorId: String
    ): InquiryStatus() {
        override val label: String = COORDINATOR_ACCEPTED
    }

    @Serializable
    class FreelancerRequested(
        val coordinatorId: String, val freelancerRequests: List<EmployeeRequest.FreelancerRequest>
    ) : InquiryStatus() {
        override val label: String = FREELANCER_REQUESTED
    }

    @Serializable
    class FreelancerAssigned(
        val coordinatorId: String, val freelancerId: String, val tags: Set<String>
    ) : InquiryStatus() {
        override val label: String = FREELANCER_ASSIGNED
    }

    @Serializable
    class InquiryResolved(
        val coordinatorId: String, val freelancerId: String, val tags: Set<String>
    ) : InquiryStatus() {
        override val label: String = INQUIRY_RESOLVED
    }
}