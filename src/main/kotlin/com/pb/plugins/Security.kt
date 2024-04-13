package com.pb.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureSecurity() {
    val jwtDomain = "pb.com"
    val jwtSecret = "secret"
    authentication {

        jwt("jwt-employee") {
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret)).withAudience("Employee").withIssuer(jwtDomain).build()
            )

            validate { credential ->
                if (!credential.payload.getClaim("employee_id").isMissing) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Invalid token")
            }
        }
    }
}
