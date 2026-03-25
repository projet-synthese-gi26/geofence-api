package ink.yowyob.geofence.controller;

import ink.yowyob.geofence.dto.request.CreateTenantRequest;
import ink.yowyob.geofence.dto.request.UpdateTenantRequest;
import ink.yowyob.geofence.dto.response.TenantListResponse;
import ink.yowyob.geofence.dto.response.TenantResponse;
import ink.yowyob.geofence.model.User;
import ink.yowyob.geofence.repository.UserRepository;
import ink.yowyob.geofence.service.TenantManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
public class TenantManagementController {

    private final TenantManagementService tenantManagementService;
    private final UserRepository userRepository;

    private Mono<User> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .flatMap(username -> Mono.fromCallable(() ->
                        userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"))
                ).subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping
    public Mono<ResponseEntity<TenantListResponse>> getAllTenants() {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> tenantManagementService.getAllTenants(user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{apiKey}")
    public Mono<ResponseEntity<TenantResponse>> getTenantByApiKey(@PathVariable String apiKey) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> tenantManagementService.getTenantByApiKey(apiKey, user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }

    @PostMapping
    public Mono<ResponseEntity<TenantResponse>> createTenant(@RequestBody CreateTenantRequest request) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> tenantManagementService.createTenant(request, user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(created -> ResponseEntity.status(201).body(created));
    }

    @PutMapping("/{apiKey}")
    public Mono<ResponseEntity<TenantResponse>> updateTenant(
            @PathVariable String apiKey,
            @RequestBody UpdateTenantRequest request
    ) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> tenantManagementService.updateTenant(apiKey, request, user))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{apiKey}")
    public Mono<ResponseEntity<Void>> deleteTenant(@PathVariable String apiKey) {
        return getCurrentUser()
                .flatMap(user -> Mono.fromCallable(() -> {
                            tenantManagementService.deleteTenant(apiKey, user);
                            return ResponseEntity.noContent().<Void>build();
                        })
                        .subscribeOn(Schedulers.boundedElastic()));
    }
}
