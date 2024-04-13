package com.pb.serializer

import com.pb.Constants
import com.pb.models.inquiry.InquiryUpdateAction
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.lang.IllegalArgumentException


object InquiryUpdateActionSerializer : JsonContentPolymorphicSerializer<InquiryUpdateAction>(InquiryUpdateAction::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<InquiryUpdateAction> {
        println("printed stuff: $element")
        return when (val label = (element as JsonObject)["label"]?.jsonPrimitive?.content) {
            Constants.PositiveInquiryUpdateActionLabels.CREATE_INQUIRY_AS_ADMIN -> {
                InquiryUpdateAction.CreateInquiryAsAdmin.serializer()
            }

            Constants.PositiveInquiryUpdateActionLabels.REQUEST_COORDINATOR_AS_ADMIN -> {
                InquiryUpdateAction.RequestCoordinatorAsAdmin.serializer()
            }

            Constants.PositiveInquiryUpdateActionLabels.REQUEST_FREELANCER_AS_COORDINATOR -> {
                InquiryUpdateAction.RequestFreelancerAsCoordinator.serializer()
            }

            Constants.PositiveInquiryUpdateActionLabels.ACCEPT_INQUIRY_AS_FREELANCER -> {
                InquiryUpdateAction.AcceptInquiryAsFreelancer.serializer()
            }

            Constants.PositiveInquiryUpdateActionLabels.ASSIGN_FREELANCER_AS_COORDINATOR -> {
                InquiryUpdateAction.AssignFreelancerAsCoordinator.serializer()
            }

            Constants.PositiveInquiryUpdateActionLabels.MARK_RESOLVED_AS_ADMIN -> {
                InquiryUpdateAction.MarkResolvedAsAdmin.serializer()
            }

            Constants.NegativeInquiryActionLabels.DELETE_INQUIRY_AS_ADMIN -> {
                InquiryUpdateAction.DeleteInquiryAsAdmin.serializer()
            }

            Constants.NegativeInquiryActionLabels.REJECT_INQUIRY_AS_COORDINATOR -> {
                InquiryUpdateAction.RejectInquiryAsCoordinator.serializer()
            }

            Constants.NegativeInquiryActionLabels.REJECT_INQUIRY_AS_FREELANCER -> {
                InquiryUpdateAction.RejectInquiryAsFreelancer.serializer()
            }

            else -> {
                throw IllegalArgumentException("IDK WHAT HAPPENED")
            }
        }
    }
}
