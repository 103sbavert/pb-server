package com.pb

object Constants {
    object InquiryStatusLabels {
        const val UNASSIGNED = "Unassigned"
        const val COORDINATOR_REQUESTED = "CoordinatorRequested"
        const val COORDINATOR_ACCEPTED = "CoordinatorAccepted"
        const val FREELANCER_REQUESTED = "FreelancerRequested"
        const val FREELANCER_ASSIGNED = "FreelancerAssigned"
        const val INQUIRY_RESOLVED = "InquiryResolved"
    }

    object PositiveInquiryUpdateActionLabels {
        const val CREATE_INQUIRY_AS_ADMIN = "CreateInquiryAsAdmin"
        const val REQUEST_COORDINATOR_AS_ADMIN = "RequestCoordinatorAsAdmin"
        const val REQUEST_FREELANCER_AS_COORDINATOR = "RequestFreelancerAsCoordinator"
        const val ACCEPT_INQUIRY_AS_FREELANCER = "AcceptInquiryAsFreelancer"
        const val ASSIGN_FREELANCER_AS_COORDINATOR = "AssignFreelancerAsCoordinator"
        const val MARK_RESOLVED_AS_ADMIN = "MarkResolvedAsAdmin" //
        const val DELETE_INQUIRY_AS_ADMIN = "DeleteInquiryAsAdmin" // must be done by an admin
        const val REJECT_INQUIRY_AS_COORDINATOR = "RejectInquiryAsCoordinator"  // must be done by a coordinator
        const val REJECT_INQUIRY_AS_FREELANCER = "RejectInquiryAsFreelancer" // must be done by a freelancer
    }
}