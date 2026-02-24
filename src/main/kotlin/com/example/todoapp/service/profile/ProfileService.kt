package com.example.todoapp.service.profile

import com.example.todoapp.api.profile.dto.ProfileResponse
import com.example.todoapp.api.profile.dto.ProfileUpdateRequest
import com.example.todoapp.domain.user.UserRepository
import com.example.todoapp.exception.ConflictException
import com.example.todoapp.exception.NotFoundException
import com.example.todoapp.logging.logger
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProfileService(
    private val userRepository: UserRepository
) {
    private val logger = logger()

    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames = ["userProfile"],
        key = "T(com.example.todoapp.cache.CacheKeys).userProfile(#userId)"
    )
    fun getMyProfile(userId: UUID): ProfileResponse {
        logger.debug("Profile get request userId={}", userId)

        val user = userRepository.findById(userId)
            .orElseThrow {
                logger.info("Profile get failed: user not found userId={}", userId)
                NotFoundException("User not found")
            }

        return ProfileResponse(
            id = user.id!!,
            email = user.email,
            username = user.username
        )
    }

    @Transactional
    @CacheEvict(
        cacheNames = ["userProfile"],
        key = "T(com.example.todoapp.cache.CacheKeys).userProfile(#userId)"
    )
    fun updateMyProfile(userId: UUID, req: ProfileUpdateRequest): ProfileResponse {
        val newUsername = req.username.trim()
        logger.info("Profile update request userId={} newUsername={}", userId, newUsername)

        val user = userRepository.findById(userId)
            .orElseThrow {
                logger.warn("Profile update failed: user not found userId={}", userId)
                NotFoundException("User not found")
            }

        if (!newUsername.equals(user.username, ignoreCase = true)) {
            if (userRepository.existsByUsername(newUsername)) {
                logger.warn("Profile update rejected: username exists userId={} username={}", userId, newUsername)
                throw ConflictException("Username already exists")
            }
            val old = user.username
            user.username = newUsername
            logger.debug("Profile username changed userId={} from={} to={}", userId, old, newUsername)
        } else {
            logger.debug("Profile update no-op (same username) userId={}", userId)
        }

        val saved = userRepository.save(user)
        logger.info("Profile update success userId={}", userId)

        return ProfileResponse(
            id = saved.id!!,
            email = saved.email,
            username = saved.username
        )
    }
}