package ink.yowyob.geofence.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @apiDefine UserPermission
 * @apiPermission user
 * @apiHeader {String} Authorization Bearer token (JWT)
 * @apiHeaderExample {json} Header-Example:
 *     {
 *       "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
 *     }
 */

/**
 * @apiDefine ErrorResponse
 * @apiError (Error 4xx) {Number} status Code d'erreur HTTP
 * @apiError (Error 4xx) {String} message Message d'erreur
 * @apiErrorExample {json} Error-Response:
 *     HTTP/1.1 400 Bad Request
 *     {
 *       "status": 400,
 *       "message": "Données invalides"
 *     }
 */

@RestController
public class DocumentationController {

    /**
     * @api {get} / Redirection vers la documentation
     * @apiName RedirectRoot
     * @apiGroup Documentation
     * @apiVersion 2.0.0
     * @apiDescription Redirige vers la documentation API
     *
     * @apiSuccessExample {json} Redirect-Response:
     *     HTTP/1.1 302 Found
     *     Location: /api/v1/docs/index.html
     */
    @GetMapping("/")
    public Mono<ResponseEntity<Void>> redirectRoot() {
        return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/api/v1/docs/index.html"))
                .build());
    }

    /**
     * @api {get} /api Redirection vers la documentation
     * @apiName RedirectApi
     * @apiGroup Documentation
     * @apiVersion 2.0.0
     * @apiDescription Redirige vers la documentation API
     *
     * @apiSuccessExample {json} Redirect-Response:
     *     HTTP/1.1 302 Found
     *     Location: /api/v1/docs/index.html
     */
    @GetMapping("/api")
    public Mono<ResponseEntity<Void>> redirectApi() {
        return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/api/v1/docs/index.html"))
                .build());
    }

    /**
     * @api {get} /api/v1/docs Redirection vers la documentation
     * @apiName RedirectDocs
     * @apiGroup Documentation
     * @apiVersion 2.0.0
     * @apiDescription Redirige vers la page principale de la documentation
     *
     * @apiSuccessExample {json} Redirect-Response:
     *     HTTP/1.1 302 Found
     *     Location: /api/v1/docs/index.html
     */
    @GetMapping("/api/v1/docs")
    public Mono<ResponseEntity<Void>> redirectDocs() {
        return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/api/v1/docs/index.html"))
                .build());
    }
}