package com.pb.models.inquiry

import com.pb.Constants.InquiryStatusLabels.COORDINATOR_ACCEPTED
import com.pb.Constants.InquiryStatusLabels.COORDINATOR_REQUESTED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_ASSIGNED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_REQUESTED
import com.pb.Constants.InquiryStatusLabels.INQUIRY_RESOLVED
import com.pb.Constants.InquiryStatusLabels.UNASSIGNED
import com.pb.serializer.InquiryStatusSerializer
import kotlinx.serialization.Serializable


@Serializable(InquiryStatusSerializer::class)
sealed class InquiryStatus {
    abstract val inquiryId: Int
    abstract val label: String

    @Serializable
    data class Unassigned(
        override val inquiryId: Int, override val label: String = UNASSIGNED
    ) : InquiryStatus()

    @Serializable
    data class CoordinatorRequested(
        override val inquiryId: Int, val requestedCoordinator: String, val assignedTime: Long, val countDownMillis: Long, override val label: String = COORDINATOR_REQUESTED

    ) : InquiryStatus()

    @Serializable
    data class CoordinatorAccepted(
        override val inquiryId: Int, val coordinator: String, override val label: String = COORDINATOR_ACCEPTED
    ) : InquiryStatus()

    @Serializable
    data class FreelancerRequested(
        override val inquiryId: Int,
        val coordinator: String,
        val freelancerFirst: String?,
        val freelancerSecond: String?,
        val freelancerThird: String?,
        val assignedTime: Long,
        val firstCountDownMillis: Long?,
        val secondCountDownMillis: Long?,
        val thirdCountDownMillis: Long?,
        val firstResponse: Boolean?,
        val secondResponse: Boolean?,
        val thirdResponse: Boolean?,
        override val label: String = FREELANCER_REQUESTED
    ) : InquiryStatus()

    @Serializable
    data class FreelancerAssigned(
        override val inquiryId: Int, val coordinator: String, val freelancer: String, override val label: String = FREELANCER_ASSIGNED
    ) : InquiryStatus()

    @Serializable
    data class InquiryResolved(
        override val inquiryId: Int, override val label: String = INQUIRY_RESOLVED, val tags: String = ""
    ) : InquiryStatus()
}