package com.emm.data.auth

class DefaultUserIdProvider(): UserIdProvider {

    override val userId: String
        get() = throw IllegalStateException()
}