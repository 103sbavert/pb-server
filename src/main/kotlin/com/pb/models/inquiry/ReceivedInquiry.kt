package com.pb.models.inquiry

import kotlinx.serialization.Serializable

@Serializable
class ReceivedInquiry(
    override val name: String,
    override val description: String,
    override val assigningMillis: Long,
    override val deadlineMillis: Long,
    override val service: String,
    override val contactNumber: String,
    override val deliveryArea: String,
    override val reference: Boolean,
): BaseInquiry()
