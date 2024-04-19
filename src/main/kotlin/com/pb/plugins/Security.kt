package com.pb.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.pb.dao.employees.AdminMDBDao
import com.pb.dao.employees.CoordinatorMDBDao
import com.pb.dao.employees.FreelancerMDBDao
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



    routing {
        post("login") {
            val credentials = runCatching { call.receive<Credentials>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            val role = credentials.employeeId.parseEmployeeId()


            val isLegal = when (role) {
                EmployeeRole.ADMIN -> {
                    val adminDb = AdminMDBDao(credentials.employeeId, MongoDb.mongoDb)
                    adminDb.verifyCredentials(credentials)
                }

                EmployeeRole.COORDINATOR -> {
                    val coordinatorDb = CoordinatorMDBDao(credentials.employeeId, MongoDb.mongoDb)
                    coordinatorDb.verifyCredentials(credentials)
                }

                EmployeeRole.FREELANCER -> {
                    val freelancerDb = FreelancerMDBDao(credentials.employeeId, MongoDb.mongoDb)
                    freelancerDb.verifyCredentials(credentials)
                }
            }
                System.err.println(isLegal)


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
                    EmployeeRole.ADMIN -> AdminMDBDao(employeeId, MongoDb.mongoDb).getSelf() != null
                    EmployeeRole.COORDINATOR -> CoordinatorMDBDao(employeeId, MongoDb.mongoDb).getSelf() != null
                    EmployeeRole.FREELANCER -> FreelancerMDBDao(employeeId, MongoDb.mongoDb).getSelf() != null
                }

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
