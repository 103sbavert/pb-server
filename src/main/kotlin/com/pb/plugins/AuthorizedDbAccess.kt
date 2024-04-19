package com.pb.plugins

import com.pb.dao.employees.AdminMDBDao
import com.pb.dao.employees.CoordinatorMDBDao
import com.pb.dao.employees.FreelancerMDBDao
import com.pb.enums.EmployeeRole
import com.pb.enums.EmployeeRole.Companion.fromString
import com.pb.enums.EmployeeRole.Companion.parseEmployeeId
import com.pb.models.employee.TransactionEmployee
import com.pb.models.inquiry.EmployeeRequest
import com.pb.models.inquiry.InquiryUpdateAction
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureAuthorizedDbAccess() {


    routing {
        authenticate("jwt-employee") {
            route("inquiries") {
                get {
                    val callerId = call.principal<JWTPrincipal>()?.subject ?: return@get call.respond(HttpStatusCode.InternalServerError)
                    val role = call.principal<JWTPrincipal>()?.get("role")?.fromString() ?: return@get call.respond(
                        HttpStatusCode.InternalServerError
                    )

                    val adminInquiriesDao = AdminMDBDao(callerId).inquiriesDao
                    val coordinatorInquiriesDao = CoordinatorMDBDao(callerId).inquiriesDao
                    val freelancerInquiriesDao = FreelancerMDBDao(callerId).inquiriesDao

                    val inquiryId = call.request.queryParameters["inquiryId"]?.toInt()
                    val inquiryStatus = call.request.queryParameters["inquiryStatus"]

                    if (inquiryId != null) {
                        val inquiry = when (role) {
                            EmployeeRole.ADMIN -> adminInquiriesDao.getInquiryById(inquiryId)
                            EmployeeRole.COORDINATOR -> coordinatorInquiriesDao.getInquiryById(inquiryId)
                            EmployeeRole.FREELANCER -> freelancerInquiriesDao.getInquiryById(inquiryId)
                        } ?: return@get call.respond(HttpStatusCode.NotFound)

                        call.respond(inquiry)
                    } else if (inquiryStatus != null) {
                        val inquiries = when (role) {
                            EmployeeRole.ADMIN -> adminInquiriesDao.getInquiriesByStatus(inquiryStatus)
                            EmployeeRole.COORDINATOR -> coordinatorInquiriesDao.getInquiriesByStatus(
                                callerId,
                                inquiryStatus
                            )

                            EmployeeRole.FREELANCER -> freelancerInquiriesDao.getInquiriesByStatus(
                                callerId,
                                inquiryStatus
                            )
                        }

                        if (inquiries == null) return@get call.respond(HttpStatusCode.Forbidden)

                        call.respond(inquiries)
                    } else {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
                get("{type}") {
                    val callerId = call.principal<JWTPrincipal>()?.subject
                        ?: return@get call.respond(HttpStatusCode.InternalServerError)
                    val role = call.principal<JWTPrincipal>()?.get("role")?.fromString() ?: return@get call.respond(HttpStatusCode.InternalServerError)
                    val type = call.parameters["type"]?.lowercase() ?: return@get call.respond(HttpStatusCode.BadRequest)

                    val adminInquiriesDao = AdminMDBDao(callerId).inquiriesDao
                    val coordinatorInquiriesDao = CoordinatorMDBDao(callerId).inquiriesDao
                    val freelancerInquiriesDao = FreelancerMDBDao(callerId).inquiriesDao

                    val inquiries = when (role) {
                        EmployeeRole.ADMIN -> when (type) {
                            "urgent" -> adminInquiriesDao.getUrgentInquiries()
                            "misc" -> adminInquiriesDao.getMiscInquiries()
                            else -> return@get call.respond(HttpStatusCode.BadRequest)
                        }

                        EmployeeRole.COORDINATOR -> when (type) {
                            "urgent" -> coordinatorInquiriesDao.getUrgentInquiries(callerId)
                            "misc" -> coordinatorInquiriesDao.getMiscInquiries(callerId)
                            else -> return@get call.respond(HttpStatusCode.BadRequest)
                        }

                        EmployeeRole.FREELANCER -> when (type) {
                            "urgent" -> freelancerInquiriesDao.getUrgentInquiries(callerId)
                            "misc" -> freelancerInquiriesDao.getMiscInquiries(callerId)
                            else -> return@get call.respond(HttpStatusCode.BadRequest)
                        }
                    }

                    System.err.println(inquiries)

                    call.respond(inquiries)
                }
                post("action") {
                    val callerId = call.principal<JWTPrincipal>()?.subject ?: return@post call.respond(HttpStatusCode.InternalServerError)
                    val callerRole = call.principal<JWTPrincipal>()?.get("role")?.fromString() ?: return@post call.respond(
                        HttpStatusCode.InternalServerError
                    )
                    val action = runCatching { call.receive<InquiryUpdateAction>() }.getOrElse {
                        it.printStackTrace()
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }

                    val adminInquiriesDao = AdminMDBDao(callerId).inquiriesDao
                    val coordinatorInquiriesDao = CoordinatorMDBDao(callerId).inquiriesDao
                    val freelancerInquiriesDao = FreelancerMDBDao(callerId).inquiriesDao

                    val result = when (callerRole) {
                        EmployeeRole.ADMIN -> when (action) {
                            is InquiryUpdateAction.RequestCoordinatorAsAdmin -> {
                                val coordinatorRequest = EmployeeRequest.CoordinatorRequest(action.requestedCoordinatorId, null, action.assignedTime, action.countDownMillis)

                                adminInquiriesDao.requestCoordinator(action.inquiryId, coordinatorRequest)
                            }

                            is InquiryUpdateAction.MarkResolvedAsAdmin -> adminInquiriesDao.markInquiryAsResolved(action.inquiryId)

                            is InquiryUpdateAction.CreateInquiryAsAdmin -> adminInquiriesDao.createInquiry(action.inquiry)

                            is InquiryUpdateAction.DeleteInquiryAsAdmin -> adminInquiriesDao.deleteInquiryById(action.inquiryId)

                            is InquiryUpdateAction.UpdateTagsAsAdmin -> adminInquiriesDao.updateTags(action.inquiryId, action.tags)

                            else -> {
                                call.respond(HttpStatusCode.Forbidden)
                                false
                            }
                        }

                        EmployeeRole.COORDINATOR -> when (action) {
                            is InquiryUpdateAction.RequestFreelancerAsCoordinator -> {
                                val freelancerRequests = action.freelancerRequests
                                coordinatorInquiriesDao.requestFreelancer(action.inquiryId, action.requestingCoordinatorId, freelancerRequests)
                            }

                            is InquiryUpdateAction.RejectInquiryAsCoordinator -> coordinatorInquiriesDao.rejectInquiry(
                                callerId,
                                action.inquiryId
                            )

                            is InquiryUpdateAction.AssignFreelancerAsCoordinator -> coordinatorInquiriesDao.assignFreelancer(
                                action.inquiryId,
                                action.assignorCoordinatorId,
                                action.assignedFreelancerIndex,
                                action.tags,
                            )

                            else -> {
                                call.respond(HttpStatusCode.Forbidden)
                                false
                            }
                        }

                        EmployeeRole.FREELANCER -> when (action) {
                            is InquiryUpdateAction.RejectInquiryAsFreelancer -> freelancerInquiriesDao.rejectInquiry(
                                callerId,
                                action.inquiryId
                            )

                            is InquiryUpdateAction.AcceptInquiryAsFreelancer -> freelancerInquiriesDao.acceptInquiry(
                                callerId,
                                action.inquiryId
                            )

                            else -> {
                                call.respond(HttpStatusCode.Forbidden)
                                false
                            }
                        }
                    }

                    if (!result) {
                        call.respond(HttpStatusCode.BadRequest)
                    } else {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

            route("employees") {
                post("create") {
                    val callerId = call.principal<JWTPrincipal>()?.subject ?: return@post call.respond(HttpStatusCode.InternalServerError)
                    val callerRole = call.principal<JWTPrincipal>()?.get("role")?.fromString() ?: return@post call.respond(
                        HttpStatusCode.InternalServerError
                    )
                    val transactionEmployee = runCatching { call.receive<TransactionEmployee>() }.getOrElse {
                        it.printStackTrace()
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }

                    if (callerRole != EmployeeRole.ADMIN) return@post call.respond(HttpStatusCode.Forbidden)

                    val adminTableDao = AdminMDBDao(callerId)

                    val isInserted = runCatching { adminTableDao.createEmployee(transactionEmployee) }.getOrElse {
                        it.printStackTrace()
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }


                    if (!isInserted) {
                        System.err.println("Employee not created")
                        call.respond(HttpStatusCode.BadRequest)
                    } else {
                        System.err.println("Employee created")
                        call.respond(HttpStatusCode.Created)
                    }
                }
                get("self") {
                    val callerId = call.principal<JWTPrincipal>()?.subject
                        ?: return@get call.respond(HttpStatusCode.InternalServerError)
                    val role = call.principal<JWTPrincipal>()?.get("role")?.fromString() ?: return@get call.respond(
                        HttpStatusCode.InternalServerError
                    )

                    val adminTableDao = AdminMDBDao(callerId)
                    val coordinatorTableDao = CoordinatorMDBDao(callerId)
                    val freelancerTableDao = FreelancerMDBDao(callerId)

                    val savedEmployee = when (role) {
                        EmployeeRole.ADMIN -> adminTableDao.getSelf()
                        EmployeeRole.COORDINATOR -> coordinatorTableDao.getSelf()
                        EmployeeRole.FREELANCER -> freelancerTableDao.getSelf()
                    } ?: call.respond(HttpStatusCode.InternalServerError)

                    call.respond(savedEmployee)
                }
                get {
                    val callerId = call.principal<JWTPrincipal>()?.subject ?: return@get call.respond(HttpStatusCode.InternalServerError)
                    val callerRole = call.principal<JWTPrincipal>()?.get("role")?.fromString() ?: return@get call.respond(HttpStatusCode.InternalServerError)
                    val role = call.request.queryParameters["role"]?.fromString()
                    val employeeId = call.request.queryParameters["employeeId"]

                    val adminTableDao = AdminMDBDao(callerId)
                    val coordinatorTableDao = CoordinatorMDBDao(callerId)

                    if (employeeId != null) {
                        val employee = when (callerRole) {
                            EmployeeRole.ADMIN -> if (employeeId.parseEmployeeId() == EmployeeRole.FREELANCER) adminTableDao.getFreelancerById(employeeId) else adminTableDao.getCoordinatorById(employeeId)
                            EmployeeRole.COORDINATOR -> coordinatorTableDao.getFreelancerById(employeeId)
                            EmployeeRole.FREELANCER -> return@get call.respond(HttpStatusCode.Forbidden)
                        } ?: call.respond(HttpStatusCode.NotFound)

                        call.respond(employee)
                    } else if (role != null) {
                        val employees = when (callerRole) {
                            EmployeeRole.ADMIN -> adminTableDao.getEmployeesByRole(role)
                            EmployeeRole.COORDINATOR -> coordinatorTableDao.getFreelancers()
                            EmployeeRole.FREELANCER -> return@get call.respond(HttpStatusCode.Forbidden)
                        }

                        call.respond(employees)
                    } else {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
        }
    }
}
