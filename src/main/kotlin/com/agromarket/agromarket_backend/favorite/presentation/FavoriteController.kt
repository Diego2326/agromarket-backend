package com.agromarket.agromarket_backend.favorite.presentation

import com.agromarket.agromarket_backend.auth.infrastructure.security.AuthenticatedUser
import com.agromarket.agromarket_backend.favorite.application.FavoriteService
import com.agromarket.agromarket_backend.publication.presentation.PublicationResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/favorites")
class FavoriteController(
    private val favoriteService: FavoriteService,
) {
    @GetMapping
    fun list(@AuthenticationPrincipal principal: AuthenticatedUser): List<PublicationResponse> =
        favoriteService.list(principal.id).map(PublicationResponse::from)

    @PostMapping("/{publicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun add(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable publicationId: UUID) {
        favoriteService.add(principal.id, publicationId)
    }

    @DeleteMapping("/{publicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(@AuthenticationPrincipal principal: AuthenticatedUser, @PathVariable publicationId: UUID) {
        favoriteService.remove(principal.id, publicationId)
    }
}
