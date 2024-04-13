package com.pb.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.pb.Constants
import com.pb.dao.EmployeeTableDao
import com.pb.dao.InquiryTableDao
import com.pb.models.Credentials
import com.pb.models.employee.EmployeeRole
import com.pb.models.employee.EmployeeRole.*
import com.pb.models.employee.EmployeeRole.Companion.parseEmployeeId
import com.pb.models.employee.ReceivedEmployee
import com.pb.models.inquiry.InquiryUpdateAction
import com.pb.models.inquiry.ReceivedInquiry
import com.pb.models.inquiry.SavedInquiry
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import java.time.Instant

fun Application.configureAuthorizedDbAccess() {
    val database = Database.connect(
        url = "jdbc:mariadb://localhost:3306/PB_db", user = "pb-server", password = "123456", driver = "org.mariadb.jdbc.Driver"
    )

    val employeeTableDao = EmployeeTableDao(database)
    val inquiryTableDao = InquiryTableDao(database)

    routing {
        post("/login") {
            val credentials = runCatching { call.receive<Credentials>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            println(credentials.employeeId)

            if (!employeeTableDao.verifyCredentials(credentials)) {
                call.respond(HttpStatusCode.Unauthorized)
                return@post
            }

            val secret = JWT.create().withAudience("Employee").withIssuer("pb.com").withClaim("employee_id", credentials.employeeId).withExpiresAt(Instant.MAX).sign(Algorithm.HMAC256("secret"))

            call.respond(hashMapOf("secret" to secret))
        }

        authenticate("jwt-employee") {
            route("/inquiries") {
                get {
                    val callerId = runCatching { call.principal<JWTPrincipal>()!!.payload.getClaim("employee_id").asString() }.getOrElse {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@get
                    }
                    val statuses = call.request.queryParameters["status"]?.split(",")

                    val inquiries = mutableListOf<SavedInquiry>()

                    if (statuses.isNullOrEmpty()) {
                        val inquiryList = when (callerId.parseEmployeeId()) {
                            ADMIN -> inquiryTableDao.getInquiriesByStatus(Constants.InquiryStatusLabels.UNASSIGNED)
                            COORDINATOR -> inquiryTableDao.getInquiryByRequestedCoordinator(callerId)
                            FREELANCER -> inquiryTableDao.getInquiryByRequestedFreelancer(callerId)
                        }

                        inquiries.addAll(inquiryList)
                    } else statuses.forEach { status ->
                        val inquiryList = inquiryTableDao.getInquiriesByStatus(status)
                        for (inquiry in inquiryList) {
                            inquiries.add(inquiry)
                        }
                    }

                    call.respond(inquiries)
                }

                post("/update-inquiry") {
                    val callerId = runCatching { call.principal<JWTPrincipal>()!!.payload.getClaim("employee_id").asString() }.getOrElse {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@post
                    }

                    val callerRole = callerId.parseEmployeeId()

                    val inquiryUpdateAction = call.receive<InquiryUpdateAction>()

                    inquiryTableDao.updateInquiry(inquiryUpdateAction)


                    call.respond(HttpStatusCode.OK)
                }

                post("create") {
                    val callerId = runCatching { call.principal<JWTPrincipal>()!!.payload.getClaim("employee_id").asString() }.getOrElse {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@post
                    }

                    val callerRole = callerId.parseEmployeeId()

                    val newEnquiry = runCatching { call.receive<ReceivedInquiry>() }.getOrElse {
                        it.printStackTrace()
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }

                    if (callerRole != ADMIN) {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@post
                    }

                    call.respond(if (inquiryTableDao.createNewInquiryAsAdmin(newEnquiry) >= 0) HttpStatusCode.Created else HttpStatusCode.InternalServerError)
                }

            }

            route("/employees") {
                get {
                    val callerRole = runCatching { call.principal<JWTPrincipal>()!!.payload.getClaim("employee_id").asString().parseEmployeeId() }.getOrElse {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@get
                    }

                    val employeeId = call.request.queryParameters["employee_id"]
                    val role = call.request.queryParameters["role"]

                    if (employeeId != null) {
                        val employee = employeeTableDao.getEmployeeById(employeeId)
                        if (employee != null) call.respond(employee)
                        else call.respond(HttpStatusCode.NotFound)
                    } else {
                        if (role != null) {
                            val users = employeeTableDao.getEmployeesByRole(EmployeeRole.valueOf(role.uppercase()))
                            call.respond(users)
                        } else {
                            call.respond(HttpStatusCode.BadRequest)
                        }
                    }
                }

                get("/self") {
                    val caller = runCatching { call.principal<JWTPrincipal>()!!.payload.getClaim("employee_id").asString() }.getOrElse {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@get
                    }

                    val employee = employeeTableDao.getEmployeeById(caller)
                    call.respond(employee!!)
                }

                post("/create") {
                    val callerRole = runCatching { call.principal<JWTPrincipal>()!!.payload.getClaim("employee_id").asString().parseEmployeeId() }.getOrElse {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@post
                    }

                    if (callerRole != ADMIN) {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@post
                    }

                    val newUser = runCatching { call.receive<ReceivedEmployee>() }.getOrElse {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }

                    if (employeeTableDao.createEmployee(newUser) != null) {
                        call.respond(HttpStatusCode.InternalServerError)
                        return@post
                    }

                    call.respond(HttpStatusCode.Created)
                }
            }

            route("/employee-status") {

                get {
                    val callerId = runCatching { call.principal<JWTPrincipal>()!!.payload.getClaim("employee_id").asString() }.getOrElse {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@get
                    }

                    val employeeId = call.request.queryParameters["employee_id"]

                    if (employeeId != null) {
                        val availabilityStatus = employeeTableDao.getEmployeeStatus(employeeId)
                        if (availabilityStatus != null) {
                            call.respond(availabilityStatus)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    } else {
                        call.respond(employeeTableDao.getEmployeeById(callerId)!!)
                        return@get
                    }
                }

                post("/update") {
                    val callerId = runCatching { call.principal<JWTPrincipal>()!!.payload.getClaim("employee_id").asString() }.getOrElse {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@post
                    }

                    val status = runCatching { call.receive<Boolean>() }.getOrElse {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }

                    employeeTableDao.setEmployeeStatus(callerId, status)

                }
            }
        }
    }
}