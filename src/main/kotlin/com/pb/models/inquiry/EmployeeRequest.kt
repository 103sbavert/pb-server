package com.pb.models.inquiry

import kotlinx.serialization.Serializable

@Serializable
sealed class EmployeeRequest {
    abstract val employeeId: String
    abstract var response: Boolean?
    abstract val requestTime: Long
    abstract val countDownTime: Long


    @Serializable
    data class FreelancerRequest(
        override val employeeId: String,
        override var response: Boolean?,
        override val requestTime: Long,
        override val countDownTime: Long
    ) : EmployeeRequest()

    @Serializable
    data class CoordinatorRequest(
        override val employeeId: String,
        override var response: Boolean?,
        override val requestTime: Long,
        override val countDownTime: Long
    ) : EmployeeRequest()
}

