package com.pb.models.inquiry

import kotlinx.serialization.Serializable

@Serializable
abstract class BaseInquiry {
    abstract val name: String
    abstract val description: String
    abstract val creationTime: Long
    abstract val deadline: Long
    abstract val service: String
    abstract val contactNumber: String
    abstract val deliveryArea: String
    abstract val reference: Boolean
}