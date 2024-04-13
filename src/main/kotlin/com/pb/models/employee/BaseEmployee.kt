package com.pb.models.employee

import kotlinx.serialization.Serializable

@Serializable
sealed class BaseEmployee {
    abstract val employeeId: String
    abstract val name: String
    abstract val emailAddress: String
    abstract val contactNumber: String
    abstract val availabilityStatus: Boolean
    abstract val role: EmployeeRole
}