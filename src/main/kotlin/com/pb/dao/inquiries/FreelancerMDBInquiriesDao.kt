package com.pb.dao.inquiries

import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.pb.Constants
import com.pb.Constants.InquiryStatusLabels.FREELANCER_ASSIGNED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_REQUESTED
import com.pb.models.inquiry.InquiryStatus
import com.pb.models.inquiry.SavedInquiry
import kotlinx.coroutines.flow.toCollection

class FreelancerMDBInquiriesDao internal constructor(database: MongoDatabase) {
    private val inquiryCollection = database.getCollection<SavedInquiry>("Inquiry")

    suspend fun getInquiryById(inquiryId: Int): SavedInquiry? {
        val inquiry = buildList {
            inquiryCollection.find(eq("inquiryId", inquiryId)).toCollection(this)
        }.firstOrNull()

        return inquiry
    }

    suspend fun getInquiriesByStatus(selfId: String, inquiryStatusLabel: String): List<SavedInquiry>? {
        if (inquiryStatusLabel !in Constants.FreelancerInquiryLabels.miscInquiries && inquiryStatusLabel !in Constants.FreelancerInquiryLabels.urgentInquiries) return null

        val filter = when (inquiryStatusLabel) {
            FREELANCER_REQUESTED -> elemMatch("status.freelancerRequests", and(eq("employeeId", selfId), eq("response", null)))
            FREELANCER_ASSIGNED -> eq("status.freelancer", selfId)
            else -> {
                throw UnknownError("DUNNO WHAT HAPPENED LOL here's some details selfID: $selfId, inquiry status: $inquiryStatusLabel")
            }
        }


        return buildList {
            inquiryCollection.find(filter).toCollection(this)
        }
    }

    suspend fun acceptInquiry(selfId: String, inquiryId: Int): Boolean {
        val inquiry = buildList { inquiryCollection.find(eq("inquiryId", inquiryId)).toCollection(this) }.firstOrNull()
        if (inquiry == null) return false
        if (inquiry.status.label != FREELANCER_REQUESTED) return false
        if (inquiry.status !is InquiryStatus.FreelancerRequested) return false

        var wasRequested = false

        inquiry.status.freelancerRequests.forEach {
            if (it.employeeId == selfId) wasRequested = true
        }

        val newFreelancerRequests =
            inquiry.status.freelancerRequests.apply { this.find { it.employeeId == selfId }?.response = true }

        val update = if (wasRequested) Updates.set("status.freelancerRequests", newFreelancerRequests)
        else return false

        return inquiryCollection.updateOne(eq("inquiryId", inquiryId), update).modifiedCount == 1L
    }


    suspend fun rejectInquiry(selfId: String, inquiryId: Int): Boolean {
        val inquiry = buildList { inquiryCollection.find(eq("inquiryId", inquiryId)).toCollection(this) }.firstOrNull()
        if (inquiry == null) return false
        if (inquiry.status.label != FREELANCER_REQUESTED) return false
        if (inquiry.status !is InquiryStatus.FreelancerRequested) return false

        var wasRequested = false

        inquiry.status.freelancerRequests.forEach {
            if (it.employeeId == selfId) wasRequested = true
        }

        val newFreelancerRequests =
            inquiry.status.freelancerRequests.apply { this.find { it.employeeId == selfId }?.response = false }

        var haveAllRejected = true

        newFreelancerRequests.forEach {
            if (it.response != false) haveAllRejected = false
            return@forEach
        }

        val update = if (haveAllRejected) Updates.set("status", InquiryStatus.CoordinatorAccepted(inquiry.status.coordinatorId)) else if (wasRequested) Updates.set("status.freelancerRequests", newFreelancerRequests)
        else return false

        return inquiryCollection.updateOne(eq("inquiryId", inquiryId), update).modifiedCount == 1L
    }

    suspend fun getMiscInquiries(selfId: String): List<SavedInquiry> {
        return getInquiriesByStatus(selfId, FREELANCER_ASSIGNED)!!
    }

    suspend fun getUrgentInquiries(selfId: String): List<SavedInquiry> {
        return getInquiriesByStatus(selfId, FREELANCER_REQUESTED)!!
    }

}