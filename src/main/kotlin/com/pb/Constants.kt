package com.pb

import com.pb.Constants.InquiryStatusLabels.COORDINATOR_ACCEPTED
import com.pb.Constants.InquiryStatusLabels.COORDINATOR_REQUESTED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_ASSIGNED
import com.pb.Constants.InquiryStatusLabels.FREELANCER_REQUESTED
import com.pb.Constants.InquiryStatusLabels.INQUIRY_RESOLVED
import com.pb.Constants.InquiryStatusLabels.UNASSIGNED

object Constants {

    val employeeIdRegex = Regex("""^[A-Z]\d{3}$""")
    const val defaultEmployeePassword = "1234"
    const val AdminCollectionName = "Admin"
    const val CoordinatorCollectionName = "Coordinator"
    const val FreelancerCollectionName = "Freelancer"

    object InquiryStatusLabels {
        const val UNASSIGNED = "Unassigned"
        const val COORDINATOR_REQUESTED = "CoordinatorRequested"
        const val COORDINATOR_ACCEPTED = "CoordinatorAccepted"
        const val FREELANCER_REQUESTED = "FreelancerRequested"
        const val FREELANCER_ASSIGNED = "FreelancerAssigned"
        const val INQUIRY_RESOLVED = "InquiryResolved"
    }

    object CoordinatorInquiryLabels {
        val miscInquiries = listOf(FREELANCER_REQUESTED, FREELANCER_ASSIGNED)
        val urgentInquiries = listOf(COORDINATOR_REQUESTED, COORDINATOR_ACCEPTED)
    }

    object AdminInquiryLabels {
        val miscInquiries = listOf(COORDINATOR_REQUESTED, COORDINATOR_ACCEPTED, FREELANCER_REQUESTED, INQUIRY_RESOLVED)
        val urgentInquiries = listOf(UNASSIGNED, FREELANCER_ASSIGNED)
    }

    object FreelancerInquiryLabels {
        val miscInquiries = listOf(FREELANCER_ASSIGNED)
        val urgentInquiries = listOf(FREELANCER_REQUESTED)
    }

    object InquiryUpdateActionLabels {
        const val CREATE_INQUIRY_AS_ADMIN = "CreateInquiryAsAdmin"
        const val REQUEST_COORDINATOR_AS_ADMIN = "RequestCoordinatorAsAdmin"
        const val UPDATE_TAGS_AS_ADMIN = "UpdateTagsAsAdmin"
        const val REQUEST_FREELANCER_AS_COORDINATOR = "RequestFreelancerAsCoordinator"
        const val ACCEPT_INQUIRY_AS_FREELANCER = "AcceptInquiryAsFreelancer"
        const val ASSIGN_FREELANCER_AS_COORDINATOR = "AssignFreelancerAsCoordinator"
        const val MARK_RESOLVED_AS_ADMIN = "MarkResolvedAsAdmin" //
        const val DELETE_INQUIRY_AS_ADMIN = "DeleteInquiryAsAdmin" // must be done by an admin
        const val REJECT_INQUIRY_AS_COORDINATOR = "RejectInquiryAsCoordinator"  // must be done by a coordinator
        const val REJECT_INQUIRY_AS_FREELANCER = "RejectInquiryAsFreelancer" // must be done by a freelancer
    }
}