package com.pb.dao.inquiries

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.pb.Constants.InquiryStatusLabels.COORDINATOR_ACCEPTED
import com.pb.Constants.InquiryStatusLabels.COORDINATOR_REQUESTED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_ASSIGNED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_REQUESTED
import com.pb.Constants.InquiryStatusLabels.INQUIRY_RESOLVED
import com.pb.Constants.InquiryStatusLabels.UNASSIGNED
import com.pb.models.inquiry.EmployeeRequest
import com.pb.models.inquiry.InquiryStatus
import com.pb.models.inquiry.ReceivedInquiry
import com.pb.models.inquiry.SavedInquiry
import kotlinx.coroutines.flow.toCollection

class AdminMDBInquiriesDao
internal constructor(database: MongoDatabase) {
    private val inquiryCollection = database.getCollection<SavedInquiry>("Inquiry")

    suspend fun getInquiryById(inquiryId: Int): SavedInquiry? {
        val inquiry = buildList {
            inquiryCollection.find(eq(inquiryId)).toCollection(this)
        }.firstOrNull()

        return inquiry
    }

    suspend fun getInquiriesByStatus(inquiryStatusLabel: String): List<SavedInquiry> {
        val filters = eq("status.label", inquiryStatusLabel)

        return buildList {
            inquiryCollection.find(filters).toCollection(this)
        }
    }

    suspend fun requestCoordinator(inquiryId: Int, coordinatorRequest: EmployeeRequest.CoordinatorRequest): Boolean {
        val status = InquiryStatus.CoordinatorRequested(coordinatorRequest)
        System.err.println(status)
        System.err.println(inquiryId)
        val update = Updates.set("status", status)
        return inquiryCollection.updateOne(eq("inquiryId", inquiryId), update).modifiedCount == 1L
    }

    suspend fun markInquiryAsResolved(inquiryId: Int): Boolean {
        val oldStatus = buildList { inquiryCollection.find(eq("inquiryId", inquiryId)).limit(1).toCollection(this) }.first().status as InquiryStatus.FreelancerAssigned
        val status = InquiryStatus.InquiryResolved(oldStatus.coordinatorId, oldStatus.freelancerId, oldStatus.tags)
        val update = Updates.set("status", status)

        return inquiryCollection.updateOne(eq("inquiryId", inquiryId), update).modifiedCount == 1L
    }

    suspend fun updateTags(inquiryId: Int, tags: Set<String>): Boolean {
        val update = Updates.set("status.tags", tags)

        return inquiryCollection.updateOne(eq("inquiryId", inquiryId), update).modifiedCount == 1L
    }

    suspend fun getMiscInquiries(): List<SavedInquiry> {
        return getInquiriesByStatus(INQUIRY_RESOLVED) + getInquiriesByStatus(FREELANCER_REQUESTED) + getInquiriesByStatus(COORDINATOR_REQUESTED) + getInquiriesByStatus(COORDINATOR_ACCEPTED)
    }

    suspend fun getUrgentInquiries(): List<SavedInquiry> {
        val inquiries = getInquiriesByStatus(FREELANCER_ASSIGNED) + getInquiriesByStatus(UNASSIGNED)
        return inquiries
    }

    suspend fun deleteInquiryById(inquiryId: Int): Boolean {
        return inquiryCollection.deleteOne(eq("inquiryId", inquiryId)).deletedCount == 1L
    }

    suspend fun createInquiry(newInquiry: ReceivedInquiry): Boolean {

        val lastId = buildList {
            inquiryCollection.find().toCollection(this)
        }.maxByOrNull { it.inquiryId }?.inquiryId

        val newId = if (lastId == null) 1 else lastId + 1

        val savedInquiry = SavedInquiry(
            newId,
            newInquiry.name,
            newInquiry.description,
            newInquiry.creationTime,
            newInquiry.deadline,
            newInquiry.service,
            newInquiry.contactNumber,
            newInquiry.deliveryArea,
            newInquiry.reference,
            InquiryStatus.Unassigned()
        )

        return inquiryCollection.insertOne(savedInquiry).insertedId != null
    }
}