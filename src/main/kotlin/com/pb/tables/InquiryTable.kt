package com.pb.tables

import com.pb.Constants
import org.jetbrains.exposed.sql.Table

object InquiryTable : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    val description = text("description")
    val assignedMillis = long("assigned_millis")
    val deadlineMillis = long("deadline_millis")
    val service = varchar("service", 100)
    val contactNumber = varchar("contact_number", 15)
    val deliveryArea = text("delivery_area")
    val reference = bool("true")
    val status = varchar("status", 20).default(Constants.InquiryStatusLabels.UNASSIGNED)
    override val primaryKey: PrimaryKey
        get() = PrimaryKey(id)
}