package com.pb.models.inquiry

import kotlinx.serialization.Serializable

@Serializable
data class SavedInquiry(
    val id: Int,
    override val name: String,
    override val description: String,
    override val creationTime: Long,
    override val deadlineMillis: Long,
    override val service: String,
    override val contactNumber: String,
    override val deliveryArea: String,
    override val reference: Boolean,
    val status: InquiryStatus
) : BaseInquiry()