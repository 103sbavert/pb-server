package com.pb.enums

import kotlinx.serialization.Serializable

@Serializable
enum class EmployeeRole {
    ADMIN,
    COORDINATOR,
    FREELANCER;

    companion object {
        fun String.fromString(): EmployeeRole {
            return when (this.lowercase()) {
                "admin" -> ADMIN
                "coordinator" -> COORDINATOR
                "freelancer" -> FREELANCER
                else -> {
                    throw IllegalArgumentException("No such role")
                }
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