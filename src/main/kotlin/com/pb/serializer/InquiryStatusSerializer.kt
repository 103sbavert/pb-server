package com.pb.serializer

import com.pb.Constants.InquiryStatusLabels.COORDINATOR_REQUESTED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_ASSIGNED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_REQUESTED
import com.pb.Constants.InquiryStatusLabels.INQUIRY_RESOLVED
import com.pb.Constants.InquiryStatusLabels.UNASSIGNED
import com.pb.models.inquiry.InquiryStatus
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bson.BsonDocument
import org.bson.codecs.kotlinx.BsonDecoder
import org.bson.codecs.kotlinx.BsonEncoder


/*object InquiryStatusSerializer : JsonContentPolymorphicSerializer<InquiryStatus>(InquiryStatus::class) {
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
}*/

object InquiryStatusBsonSerializer : KSerializer<InquiryStatus> {
    override fun deserialize(decoder: Decoder): InquiryStatus {
        if (decoder is BsonDecoder) {
            val mark = decoder.reader().mark
            val document = (decoder.decodeBsonValue() as BsonDocument)["label"]?.asString()?.value
            mark.reset()

            val obj = when (document) {
                COORDINATOR_REQUESTED -> decoder.decodeSerializableValue(InquiryStatus.CoordinatorRequested.serializer())
                FREELANCER_ASSIGNED -> decoder.decodeSerializableValue(InquiryStatus.FreelancerAssigned.serializer())
                FREELANCER_REQUESTED -> decoder.decodeSerializableValue(InquiryStatus.FreelancerRequested.serializer())
                INQUIRY_RESOLVED -> decoder.decodeSerializableValue(InquiryStatus.InquiryResolved.serializer())
                UNASSIGNED -> decoder.decodeSerializableValue(InquiryStatus.Unassigned.serializer())
                else -> throw SerializationException("Unknown label")
            }

            return obj
        }
        throw IllegalStateException("Wrong decoder")
    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("InquiryStatus")

    override fun serialize(encoder: Encoder, value: InquiryStatus) {
        when (value) {
            is InquiryStatus.Unassigned -> encoder.encodeSerializableValue(InquiryStatus.Unassigned.serializer(), value)
            is InquiryStatus.CoordinatorRequested -> encoder.encodeSerializableValue(InquiryStatus.CoordinatorRequested.serializer(), value)
            is InquiryStatus.CoordinatorAccepted -> encoder.encodeSerializableValue(InquiryStatus.CoordinatorAccepted.serializer(), value)
            is InquiryStatus.FreelancerRequested -> encoder.encodeSerializableValue(InquiryStatus.FreelancerRequested.serializer(), value)
            is InquiryStatus.FreelancerAssigned -> encoder.encodeSerializableValue(InquiryStatus.FreelancerAssigned.serializer(), value)
            is InquiryStatus.InquiryResolved -> encoder.encodeSerializableValue(InquiryStatus.InquiryResolved.serializer(), value)
        }
    }
}