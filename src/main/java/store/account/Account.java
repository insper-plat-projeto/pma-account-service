package store.account;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

// Serializable: the Redis cache stores it with Java serialization
@Builder @Data
@Accessors(fluent = true)
public class Account implements Serializable {

    private String id;
    private String name;
    private String email;
    private String password;
    private String hashPassword;

}