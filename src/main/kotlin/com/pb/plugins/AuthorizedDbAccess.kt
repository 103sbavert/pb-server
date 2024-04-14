package com.pb.plugins

import com.pb.dao.employees.AdminTableDao
import com.pb.dao.employees.CoordinatorTableDao
import com.pb.dao.employees.FreelancerTableDao
import com.pb.dao.inquiries.AdminInquiriesDao
import com.pb.dao.inquiries.CoordinatorInquiriesDao
import com.pb.dao.inquiries.FreelancerInquiriesDao
import com.pb.enums.EmployeeRole
import com.pb.enums.EmployeeRole.Companion.fromString
import com.pb.models.employee.ReceivedEmployee
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

            val adminTableDao = AdminTableDao(database)
            val adminInquiriesDao = AdminInquiriesDao(database)

            val coordinatorTableDao = CoordinatorTableDao(database)
            val coordinatorInquiriesDao = CoordinatorInquiriesDao(database)

            val freelancerTableDao = FreelancerTableDao(database)
            val freelancerInquiriesDao = FreelancerInquiriesDao(database)

            route("inquiries") {
                get {
                    val callerId = call.principal<JWTPrincipal>()?.subject ?: return@get call.respond(HttpStatusCode.InternalServerError)
                    val role = call.principal<JWTPrincipal>()?.get("role")?.fromString() ?: return@get call.respond(HttpStatusCode.InternalServerError)
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
                            EmployeeRole.COORDINATOR -> coordinatorInquiriesDao.getInquiriesByStatus(callerId, inquiryStatus)
                            EmployeeRole.FREELANCER -> freelancerInquiriesDao.getInquiriesByStatus(callerId, inquiryStatus)
                        }

                        call.respond(inquiries)
                    } else {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
                get("{type}") {
                    val callerId = call.principal<JWTPrincipal>()?.subject ?: return@get call.respond(HttpStatusCode.InternalServerError)
                    val role = call.principal<JWTPrincipal>()?.get("role")?.fromString() ?: return@get call.respond(HttpStatusCode.InternalServerError)
                    val type = call.parameters["type"]?.lowercase() ?: return@get call.respond(HttpStatusCode.BadRequest)

                    val inquiries = when (role) {
                        EmployeeRole.ADMIN -> if (type == "urgent") adminInquiriesDao.getUrgentInquiries() else if (type == "misc") adminInquiriesDao.getMiscInquiries() else return@get call.respond(HttpStatusCode.BadRequest)
                        EmployeeRole.COORDINATOR -> if (type == "urgent") coordinatorInquiriesDao.getUrgentInquiries(callerId) else if (type == "misc") coordinatorInquiriesDao.getMiscInquiries(callerId) else return@get call.respond(HttpStatusCode.BadRequest)
                        EmployeeRole.FREELANCER -> if (type == "urgent") freelancerInquiriesDao.getUrgentInquiries(callerId) else if (type == "misc") freelancerInquiriesDao.getMiscInquiries(callerId) else return@get call.respond(HttpStatusCode.BadRequest)
                    }

                    call.respond(inquiries)
                }
                post("action") {
                    val callerId = call.principal<JWTPrincipal>()?.subject ?: return@post call.respond(HttpStatusCode.InternalServerError)
                    val role = call.principal<JWTPrincipal>()?.get("role")?.fromString() ?: return@post call.respond(HttpStatusCode.InternalServerError)
                    val action = runCatching { call.receive<InquiryUpdateAction>() }.getOrElse {
                        it.printStackTrace()
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }

                    when (role) {
                        EmployeeRole.ADMIN -> when (action) {
                            is InquiryUpdateAction.RequestCoordinatorAsAdmin -> adminInquiriesDao.requestCoordinator(action.inquiryId, action.requestedCoordinatorId, action.assignedTime, action.countDownMillis)
                            is InquiryUpdateAction.MarkResolvedAsAdmin -> adminInquiriesDao.markInquiryAsResolved(action.inquiryId, action.tags)
                            is InquiryUpdateAction.CreateInquiryAsAdmin -> adminInquiriesDao.createInquiry(action.inquiry)
                            is InquiryUpdateAction.DeleteInquiryAsAdmin -> adminInquiriesDao.deleteInquiry(action.inquiryId)
                            else -> return@post call.respond(HttpStatusCode.Forbidden)
                        }

                        EmployeeRole.COORDINATOR -> when (action) {
                            is InquiryUpdateAction.RequestFreelancerAsCoordinator -> coordinatorInquiriesDao.requestFreelancer(callerId, action.inquiryId, action.requestedFreelancerId, action.assignedTime, action.countDownMillis)
                            is InquiryUpdateAction.RejectInquiryAsCoordinator -> coordinatorInquiriesDao.rejectRequestedInquiry(callerId, action.inquiryId)
                            is InquiryUpdateAction.AssignFreelancerAsCoordinator -> coordinatorInquiriesDao.assignFreelancer(callerId, action.inquiryId, action.assignedFreelancerId)
                            else -> call.respond(HttpStatusCode.Forbidden)
                        }

                        EmployeeRole.FREELANCER -> when (action) {
                            is InquiryUpdateAction.RejectInquiryAsFreelancer -> freelancerInquiriesDao.rejectInquiry(callerId, action.inquiryId)
                            is InquiryUpdateAction.AcceptInquiryAsFreelancer -> freelancerInquiriesDao.acceptInquiry(callerId, action.inquiryId)
                            else -> call.respond(HttpStatusCode.Forbidden)
                        }
                    }
                }
            }

            route("employees") {
                post("create") {
                    val role = call.principal<JWTPrincipal>()?.get("role")?.fromString() ?: return@post call.respond(HttpStatusCode.InternalServerError)
                    val receivedEmployee = runCatching { call.receive<ReceivedEmployee>() }.getOrElse {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }

                    if (role != EmployeeRole.ADMIN) return@post call.respond(HttpStatusCode.Forbidden)

                    if (adminTableDao.createEmployee(receivedEmployee) == -1) {
                        call.respond(HttpStatusCode.InternalServerError)
                    } else {
                        call.respond(HttpStatusCode.Created)
                    }
                }
                get("self") {
                    val callerId = call.principal<JWTPrincipal>()?.subject ?: return@get call.respond(HttpStatusCode.InternalServerError)
                    val role = call.principal<JWTPrincipal>()?.get("role")?.fromString() ?: return@get call.respond(HttpStatusCode.InternalServerError)
                    val savedEmployee = when (role) {
                        EmployeeRole.ADMIN -> adminTableDao.getSelf(callerId)
                        EmployeeRole.COORDINATOR -> coordinatorTableDao.getSelf(callerId)
                        EmployeeRole.FREELANCER -> freelancerTableDao.getSelf(callerId)
                    } ?: call.respond(HttpStatusCode.InternalServerError)

                    call.respond(savedEmployee)
                }
                get {
                    val callerRole = call.principal<JWTPrincipal>()?.get("role")?.fromString() ?: return@get call.respond(HttpStatusCode.InternalServerError)
                    val role = call.request.queryParameters["role"]?.fromString()
                    val employeeId = call.request.queryParameters["employeeId"]

                    if (employeeId != null) {
                        val employee = when (callerRole) {
                            EmployeeRole.ADMIN -> adminTableDao.getEmployeeById(employeeId)
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
