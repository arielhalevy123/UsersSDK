package io.github.arielhalevy123.userssdk

import org.junit.Assert.assertEquals
import org.junit.Test

class BaseUrlTest {

    @Test
    fun addsTrailingSlash() {
        assertEquals("https://api.example.com/", UsersSdk.normalizeBaseUrl("https://api.example.com"))
    }

    @Test
    fun keepsSingleTrailingSlashAndTrims() {
        assertEquals("https://api.example.com/", UsersSdk.normalizeBaseUrl("  https://api.example.com//  "))
    }

    @Test
    fun acceptsHttpForLocalDevelopment() {
        assertEquals("http://10.0.2.2:8080/", UsersSdk.normalizeBaseUrl("http://10.0.2.2:8080"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingScheme() {
        UsersSdk.normalizeBaseUrl("api.example.com")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnreplacedPlaceholder() {
        UsersSdk.normalizeBaseUrl("https://<railway-domain>/")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmpty() {
        UsersSdk.normalizeBaseUrl("   ")
    }
}
