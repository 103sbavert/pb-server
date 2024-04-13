package com.pb.serializer

import com.pb.Constants.InquiryStatusLabels.COORDINATOR_REQUESTED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_ASSIGNED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_REQUESTED
import com.pb.Constants.InquiryStatusLabels.INQUIRY_RESOLVED
import com.pb.Constants.InquiryStatusLabels.UNASSIGNED
import com.pb.models.inquiry.InquiryStatus
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

object InquiryStatusSerializer : JsonContentPolymorphicSerializer<InquiryStatus>(InquiryStatus::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<InquiryStatus> {
        return when (val label = (element as JsonObject)["label"]?.jsonPrimitive?.content) {
            UNASSIGNED -> InquiryStatus.Unassigned.serializer()
            COORDINATOR_REQUESTED -> InquiryStatus.CoordinatorRequested.serializer()
            FREELANCER_REQUESTED -> InquiryStatus.FreelancerRequested.serializer()
            FREELANCER_ASSIGNED -> InquiryStatus.FreelancerAssigned.serializer()
            INQUIRY_RESOLVED -> InquiryStatus.InquiryResolved.serializer()
            else -> throw IllegalArgumentException("Not a valid status $label")
        }
    }
}
