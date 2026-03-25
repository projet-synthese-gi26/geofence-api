package ink.yowyob.geofence.dto.request.AuthRequest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LoginUsernameDTO.class, name = "username"),
        @JsonSubTypes.Type(value = LoginEmailDTO.class, name = "email"),
        @JsonSubTypes.Type(value = LoginPhoneNumberDTO.class, name = "phone")
})
public interface LoginDTO {
    String password();
}
