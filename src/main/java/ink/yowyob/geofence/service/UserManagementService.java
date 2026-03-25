package ink.yowyob.geofence.service;

import ink.yowyob.geofence.dto.request.UpdateUserRoleRequest;
import ink.yowyob.geofence.dto.response.UserDTO;
import ink.yowyob.geofence.dto.response.UserListResponse;
import ink.yowyob.geofence.model.User;

import java.util.UUID;

public interface UserManagementService {
    UserListResponse getAllUsers(Integer page, Integer size, User currentUser);
    UserDTO getUserById(UUID id, User currentUser);
    UserDTO updateUserRole(UUID id, UpdateUserRoleRequest request, User admin);
    void deleteUser(UUID id, User admin);
    UserListResponse getUsersForSharing(User currentUser);
}