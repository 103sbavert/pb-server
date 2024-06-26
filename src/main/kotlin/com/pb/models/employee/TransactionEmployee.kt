package com.pb.models.employee

import com.pb.enums.EmployeeRole
import kotlinx.serialization.Serializable

@Serializable
data class TransactionEmployee(
    override val employeeId: String,
    override val name: String,
    override val emailAddress: String,
    override val contactNumber: String,
    override val availabilityStatus: Boolean,
    override val role: EmployeeRole
) : BaseEmployee()