package ink.yowyob.geofence.auth;

import ink.yowyob.geofence.dto.request.AuthRequest.*;
import ink.yowyob.geofence.dto.response.AuthResponse;
import ink.yowyob.geofence.dto.response.RegisterResponse;
import ink.yowyob.geofence.model.User;

public interface AuthService {
    RegisterResponse register(RegisterDTO registerDTO, String tenantId);

    AuthResponse login(LoginDTO loginDTO, String tenantId);

    User loginEmail(LoginEmailDTO loginEmailDTO, String tenantId);
    User loginUsername(LoginUsernameDTO loginUsernameDTO, String tenantId);
    User loginPhone(LoginPhoneNumberDTO loginPhoneNumberDTO, String tenantId);
    AuthResponse getCurrentUser(User user);
}
