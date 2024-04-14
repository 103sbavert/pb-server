package com.pb.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.pb.dao.employees.AdminTableDao
import com.pb.dao.employees.CoordinatorTableDao
import com.pb.dao.employees.FreelancerTableDao
import com.pb.enums.EmployeeRole
import com.pb.enums.EmployeeRole.Companion.fromString
import com.pb.enums.EmployeeRole.Companion.parseEmployeeId
import com.pb.models.Credentials
import com.pb.pb_app.data.models.Token
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureSecurity() {
    val issuer = environment.config.property("jwt.issuer").getString()
    val realm = environment.config.property("jwt.realm").getString()
    val secret = environment.config.property("jwt.secret").getString()
    val adminTableDao = AdminTableDao(database)
    val coordinatorTableDao = CoordinatorTableDao(database)
    val freelancerTableDao = FreelancerTableDao(database)


    routing {
        post("login") {
            val credentials = runCatching { call.receive<Credentials>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            val isLegal = when (credentials.employeeId.parseEmployeeId()) {
                EmployeeRole.ADMIN -> AdminTableDao(database).verifyCredentials(credentials)
                EmployeeRole.COORDINATOR -> CoordinatorTableDao(database).verifyCredentials(credentials)
                EmployeeRole.FREELANCER -> FreelancerTableDao(database).verifyCredentials(credentials)
            }


            if (isLegal) {
                val token = Token(
                    JWT.create()
                        .withIssuer(issuer)
                        .withSubject(credentials.employeeId)
                        .withAudience(credentials.employeeId.parseEmployeeId().name)
                        .withClaim("role", credentials.employeeId.parseEmployeeId().name)
                        .sign(Algorithm.HMAC256(secret))
                )
                call.respond(token)
            } else {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }

    authentication {
        jwt("jwt-employee") {
            this.realm = realm
            val validAudiences = EmployeeRole.entries.map { it.name }.toTypedArray()

            verifier {
                JWT.require(Algorithm.HMAC256(secret)).withAnyOfAudience(*validAudiences).withIssuer(issuer).build()
            }

            validate { credential ->
                val employeeId = credential.subject
                val role = credential["role"]?.fromString()
                if (employeeId.isNullOrBlank()) return@validate null
                if (role == null) return@validate null

                val isValid = when (role) {
                    EmployeeRole.ADMIN -> adminTableDao.getSelf(employeeId) != null
                    EmployeeRole.COORDINATOR -> coordinatorTableDao.getSelf(employeeId) != null
                    EmployeeRole.FREELANCER -> freelancerTableDao.getSelf(employeeId) != null
                }

                System.err.println("conf security" + credential)
                System.err.println("conf security" + isValid)

                if (isValid) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Invalid token received")
            }
        }
    }
}
