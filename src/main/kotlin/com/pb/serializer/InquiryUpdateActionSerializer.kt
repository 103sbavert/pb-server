package com.pb.serializer

import com.pb.Constants.PositiveInquiryUpdateActionLabels.ACCEPT_INQUIRY_AS_FREELANCER
import com.pb.Constants.PositiveInquiryUpdateActionLabels.ASSIGN_FREELANCER_AS_COORDINATOR
import com.pb.Constants.PositiveInquiryUpdateActionLabels.CREATE_INQUIRY_AS_ADMIN
import com.pb.Constants.PositiveInquiryUpdateActionLabels.DELETE_INQUIRY_AS_ADMIN
import com.pb.Constants.PositiveInquiryUpdateActionLabels.MARK_RESOLVED_AS_ADMIN
import com.pb.Constants.PositiveInquiryUpdateActionLabels.REJECT_INQUIRY_AS_COORDINATOR
import com.pb.Constants.PositiveInquiryUpdateActionLabels.REJECT_INQUIRY_AS_FREELANCER
import com.pb.Constants.PositiveInquiryUpdateActionLabels.REQUEST_COORDINATOR_AS_ADMIN
import com.pb.Constants.PositiveInquiryUpdateActionLabels.REQUEST_FREELANCER_AS_COORDINATOR
import com.pb.models.inquiry.InquiryUpdateAction
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive


object InquiryUpdateActionSerializer : JsonContentPolymorphicSerializer<InquiryUpdateAction>(InquiryUpdateAction::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<InquiryUpdateAction> {
        println("printed stuff: $element")
        return when ((element as JsonObject)["label"]?.jsonPrimitive?.content) {
            CREATE_INQUIRY_AS_ADMIN -> {
                InquiryUpdateAction.CreateInquiryAsAdmin.serializer()
            }

            REQUEST_COORDINATOR_AS_ADMIN -> {
                InquiryUpdateAction.RequestCoordinatorAsAdmin.serializer()
            }

            REQUEST_FREELANCER_AS_COORDINATOR -> {
                InquiryUpdateAction.RequestFreelancerAsCoordinator.serializer()
            }

            ACCEPT_INQUIRY_AS_FREELANCER -> {
                InquiryUpdateAction.AcceptInquiryAsFreelancer.serializer()
            }

            ASSIGN_FREELANCER_AS_COORDINATOR -> {
                InquiryUpdateAction.AssignFreelancerAsCoordinator.serializer()
            }

            MARK_RESOLVED_AS_ADMIN -> {
                InquiryUpdateAction.MarkResolvedAsAdmin.serializer()
            }

            DELETE_INQUIRY_AS_ADMIN -> {
                InquiryUpdateAction.DeleteInquiryAsAdmin.serializer()
            }

            REJECT_INQUIRY_AS_COORDINATOR -> {
                InquiryUpdateAction.RejectInquiryAsCoordinator.serializer()
            }

            REJECT_INQUIRY_AS_FREELANCER -> {
                InquiryUpdateAction.RejectInquiryAsFreelancer.serializer()
            }

            else -> {
                throw IllegalArgumentException("IDK WHAT HAPPENED")
            }
        }
    }
}
