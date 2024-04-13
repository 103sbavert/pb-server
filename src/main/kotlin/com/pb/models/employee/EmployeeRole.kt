package com.pb.models.employee

import com.pb.tables.*
import kotlinx.serialization.Serializable

@Serializable
enum class EmployeeRole() {
    ADMIN,
    COORDINATOR,
    FREELANCER;

    companion object {
        val EmployeeRole.statusTables: List<InquiryStatusTable>
            get() {
                return when (this) {
                    ADMIN -> listOf(UnassignedTable, CoordinatorRequestedTable, FreelancerRequestedTable, FreelancerAssignedTable, InquiryResolvedTable)
                    COORDINATOR -> listOf(FreelancerRequestedTable, FreelancerAssignedTable)
                    FREELANCER -> emptyList()
                }
            }

        val EmployeeRole.table: EmployeeTable
            get() {
                return when (this) {
                    ADMIN -> AdminTable
                    COORDINATOR -> CoordinatorTable
                    FREELANCER -> FreelancerTable
                }
            }

        fun String.parseEmployeeId(): EmployeeRole {
            return if (this.startsWith("PB-AM")) {
                ADMIN
            } else if (this.startsWith("PB-PC")) {
                COORDINATOR
            } else if (this.startsWith("PB-FR")) {
                FREELANCER
            } else {
                throw IllegalArgumentException("Illegal Username provided")
            }
        }
    }
}