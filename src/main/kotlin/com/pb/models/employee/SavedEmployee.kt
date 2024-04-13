package com.pb.models.employee

import kotlinx.serialization.Serializable

@Serializable
data class SavedEmployee(
    override val employeeId: String,
    override val name: String,
    override val emailAddress: String,
    override val contactNumber: String,
    override val availabilityStatus: Boolean,
    override val role: EmployeeRole,
) : BaseEmployee() {
}