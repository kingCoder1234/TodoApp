package com.example.todoapp.exception

open class ApiException(
    message: String,
    val statusCode: Int
) : RuntimeException(message)

class UnauthorizedException(message: String = "Unauthorized") : ApiException(message, 401)

class ForbiddenException(message: String = "Forbidden") : ApiException(message, 403)

class NotFoundException(message: String = "Not found") : ApiException(message, 404)

class ConflictException(message: String = "Conflict") : ApiException(message, 409)

class BadRequestException(message: String = "Bad request") : ApiException(message, 400)