package com.pb.models.employee

import com.pb.enums.EmployeeRole
import kotlinx.serialization.Serializable

@Serializable
abstract class BaseEmployee {
    abstract val employeeId: String
    abstract val name: String
    abstract val emailAddress: String
    abstract val contactNumber: String
    abstract val availabilityStatus: Boolean
    abstract val role: EmployeeRole
}