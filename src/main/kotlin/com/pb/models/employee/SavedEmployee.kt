package com.pb.models.employee

import com.pb.enums.EmployeeRole
import kotlinx.serialization.Serializable

@Serializable
data class SavedEmployee(
    override val employeeId: String,
    override val name: String,
    override val emailAddress: String,
    val password: String = "1234",
    override val contactNumber: String,
    override val availabilityStatus: Boolean,
    override val role: EmployeeRole
) : BaseEmployee()