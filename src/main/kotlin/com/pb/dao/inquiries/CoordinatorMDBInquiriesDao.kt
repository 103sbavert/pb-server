package com.pb.dao.inquiries

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.pb.Constants
import com.pb.Constants.InquiryStatusLabels.COORDINATOR_ACCEPTED
import com.pb.Constants.InquiryStatusLabels.COORDINATOR_REQUESTED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_ASSIGNED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_REQUESTED
import com.pb.models.inquiry.EmployeeRequest
import com.pb.models.inquiry.InquiryStatus
import com.pb.models.inquiry.SavedInquiry
import kotlinx.coroutines.flow.toCollection

class CoordinatorMDBInquiriesDao internal constructor(database: MongoDatabase) {
    private val inquiryCollection = database.getCollection<SavedInquiry>("Inquiry")

    suspend fun getInquiryById(inquiryId: Int): SavedInquiry? {
        val inquiry = buildList {
            inquiryCollection.find(eq("inquiryId", inquiryId)).toCollection(this)
        }.firstOrNull()

        return inquiry
    }

    suspend fun getInquiriesByStatus(selfId: String, inquiryStatusLabel: String): List<SavedInquiry>? {
        if (inquiryStatusLabel !in Constants.CoordinatorInquiryLabels.miscInquiries && inquiryStatusLabel !in Constants.CoordinatorInquiryLabels.urgentInquiries) return null

        val filter = when (inquiryStatusLabel) {
            COORDINATOR_REQUESTED -> and(eq("status.coordinatorRequest.employeeId", selfId), eq("status.label", inquiryStatusLabel))
            COORDINATOR_ACCEPTED -> and(eq("status.coordinatorId", selfId), eq("status.label", inquiryStatusLabel))
            FREELANCER_REQUESTED -> and(eq("status.coordinatorId", selfId), eq("status.label", inquiryStatusLabel))
            FREELANCER_ASSIGNED -> and(eq("status.coordinatorId", selfId), eq("status.label", inquiryStatusLabel))
            else -> {
                throw UnknownError("DUNNO WHAT HAPPENED LOL here's some details selfID: $selfId, inquiry status: $inquiryStatusLabel")
            }
        }


        val collection = buildList {
            inquiryCollection.find(filter).toCollection(this)
        }

        System.err.println(collection.size)

        return collection
    }

    suspend fun requestFreelancer(
        inquiryId: Int, assignorCoordinatorId: String, freelancerRequests: List<EmployeeRequest.FreelancerRequest>
    ): Boolean {
        val inquiry = buildList { inquiryCollection.find(eq("inquiryId", inquiryId)).toCollection(this) }.firstOrNull()
        if (inquiry == null) return false
        if (inquiry.status.label != COORDINATOR_REQUESTED && inquiry.status.label != COORDINATOR_ACCEPTED) return false

        val update = if (inquiry.status is InquiryStatus.FreelancerRequested) {
            Updates.set("status.freelancerRequests", freelancerRequests)
        } else {
            val inquiryStatus = InquiryStatus.FreelancerRequested(assignorCoordinatorId, freelancerRequests)
            Updates.set("status", inquiryStatus)
        }

        return inquiryCollection.updateOne(eq("inquiryId", inquiryId), update).modifiedCount == 1L
    }

    suspend fun assignFreelancer(
        inquiryId: Int, assignorCoordinatorId: String, assignedFreelancerIndex: Int, tags: Set<String>
    ): Boolean {
        val inquiry = buildList { inquiryCollection.find(eq("inquiryId", inquiryId)).toCollection(this) }.firstOrNull()
        if (inquiry == null) return false
        if (inquiry.status.label != FREELANCER_REQUESTED) return false

        if (inquiry.status !is InquiryStatus.FreelancerRequested) return false

        val freelancerId =
            if (inquiry.status.freelancerRequests[assignedFreelancerIndex].response == true) inquiry.status.freelancerRequests[assignedFreelancerIndex].employeeId
            else return false

        val status = InquiryStatus.FreelancerAssigned(assignorCoordinatorId, freelancerId, tags)
        val update = Updates.set("status", status)
        return inquiryCollection.updateOne(eq("inquiryId", inquiryId), update).modifiedCount == 1L
    }

    suspend fun rejectInquiry(selfId: String, inquiryId: Int): Boolean {
        val inquiry = buildList { inquiryCollection.find(eq("inquiryId", inquiryId)).toCollection(this) }.firstOrNull()
        if (inquiry == null) return false

        if (inquiry.status.label != COORDINATOR_REQUESTED) return false
        if (inquiry.status !is InquiryStatus.CoordinatorRequested) return false
        if (inquiry.status.coordinatorRequest.employeeId != selfId) return false

        val status = InquiryStatus.Unassigned()
        val update = Updates.set("status", status)

        return inquiryCollection.updateOne(eq("inquiryId", inquiryId), update).modifiedCount == 1L
    }

    suspend fun getMiscInquiries(selfId: String): List<SavedInquiry> {
        val inquiries = getInquiriesByStatus(selfId, FREELANCER_REQUESTED)!! +
                getInquiriesByStatus(selfId, FREELANCER_ASSIGNED)!!

        for (inquiry in inquiries) {
            System.err.println(inquiry.status)
        }

        return inquiries.filter { each ->
            if (each.status !is InquiryStatus.FreelancerRequested) return@filter true
            each.status.freelancerRequests.forEach { if (it.response == null) return@filter true }
            return@filter false
        }
    }

    suspend fun getUrgentInquiries(selfId: String): List<SavedInquiry> {
        val inquiries = getInquiriesByStatus(selfId, COORDINATOR_REQUESTED)!! +
                getInquiriesByStatus(selfId, FREELANCER_REQUESTED)!! + getInquiriesByStatus(selfId, COORDINATOR_ACCEPTED)!!

        return inquiries.filter { each ->
            if (each.status !is InquiryStatus.FreelancerRequested) return@filter true

            each.status.freelancerRequests.forEach {
                System.err.println(it.response)
                if (it.response == null) return@filter false
            }
            return@filter true
        }
    }

}