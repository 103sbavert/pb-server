package com.pb.models.inquiry

import com.pb.Constants.InquiryUpdateActionLabels.ACCEPT_INQUIRY_AS_FREELANCER
import com.pb.Constants.InquiryUpdateActionLabels.ASSIGN_FREELANCER_AS_COORDINATOR
import com.pb.Constants.InquiryUpdateActionLabels.CREATE_INQUIRY_AS_ADMIN
import com.pb.Constants.InquiryUpdateActionLabels.DELETE_INQUIRY_AS_ADMIN
import com.pb.Constants.InquiryUpdateActionLabels.MARK_RESOLVED_AS_ADMIN
import com.pb.Constants.InquiryUpdateActionLabels.REJECT_INQUIRY_AS_COORDINATOR
import com.pb.Constants.InquiryUpdateActionLabels.REJECT_INQUIRY_AS_FREELANCER
import com.pb.Constants.InquiryUpdateActionLabels.REQUEST_COORDINATOR_AS_ADMIN
import com.pb.Constants.InquiryUpdateActionLabels.REQUEST_FREELANCER_AS_COORDINATOR
import com.pb.Constants.InquiryUpdateActionLabels.UPDATE_TAGS_AS_ADMIN
import com.pb.serializer.InquiryUpdateActionSerializer
import kotlinx.serialization.Serializable


@Serializable(InquiryUpdateActionSerializer::class)
sealed class InquiryUpdateAction {
    abstract val label: String
    abstract val inquiryId: Int


    @Serializable
    data class CreateInquiryAsAdmin(val inquiry: ReceivedInquiry) : InquiryUpdateAction() {
        override val label: String = CREATE_INQUIRY_AS_ADMIN
        override val inquiryId = -1
    }

    @Serializable
    data class DeleteInquiryAsAdmin(val deletingAdminId: String, override val inquiryId: Int) : InquiryUpdateAction() {
        override val label: String = DELETE_INQUIRY_AS_ADMIN
    }

    @Serializable
    data class RequestCoordinatorAsAdmin(val requestingAdminId: String, val requestedCoordinatorId: String, override val inquiryId: Int, val assignedTime: Long, val countDownMillis: Long) : InquiryUpdateAction() {
        override val label = REQUEST_COORDINATOR_AS_ADMIN
    }

    @Serializable
    data class UpdateTagsAsAdmin(override val inquiryId: Int, val tags: Set<String>) : InquiryUpdateAction() {
        override val label: String = UPDATE_TAGS_AS_ADMIN
    }

    @Serializable
    data class MarkResolvedAsAdmin(val markingAdminId: String, override val inquiryId: Int, val tags: Set<String>) : InquiryUpdateAction() {
        override val label: String = MARK_RESOLVED_AS_ADMIN
    }

    @Serializable
    data class RequestFreelancerAsCoordinator(val requestingCoordinatorId: String, override val inquiryId: Int, val freelancerRequests: List<EmployeeRequest.FreelancerRequest>) : InquiryUpdateAction() {
        override val label = REQUEST_FREELANCER_AS_COORDINATOR
    }

    @Serializable
    data class RejectInquiryAsCoordinator(val rejectingCoordinatorId: String, override val inquiryId: Int) : InquiryUpdateAction() {
        override val label: String = REJECT_INQUIRY_AS_COORDINATOR
    }

    @Serializable
    data class AssignFreelancerAsCoordinator(val assignorCoordinatorId: String, val assignedFreelancerIndex: Int, override val inquiryId: Int, val tags: Set<String>) : InquiryUpdateAction() {
        override val label: String = ASSIGN_FREELANCER_AS_COORDINATOR
    }

    @Serializable
    data class AcceptInquiryAsFreelancer(val acceptorFreelancerId: String, override val inquiryId: Int) : InquiryUpdateAction() {
        override val label: String = ACCEPT_INQUIRY_AS_FREELANCER
    }

    @Serializable
    data class RejectInquiryAsFreelancer(val rejectingFreelancerId: String, override val inquiryId: Int) : InquiryUpdateAction() {
        override val label = REJECT_INQUIRY_AS_FREELANCER
    }
}