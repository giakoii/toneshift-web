package vn.toneshift.api.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.toneshift.api.auth.entity.AuthProvider;
import vn.toneshift.api.common.dto.UserRef;
import vn.toneshift.api.common.enums.AuthProviderType;
import vn.toneshift.api.user.dto.UserResponse;
import vn.toneshift.api.user.entity.User;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Maps {@link User} entities to the API representations.
 */
@Mapper
public interface UserMapper {

    /**
     * Maps a user to the full response, including which providers are linked.
     *
     * @param user the user to map, with {@code providers} loaded
     * @return the user response
     */
    @Mapping(target = "image", source = "imageUrl")
    @Mapping(target = "hasPassword", expression = "java(user.hasPassword())")
    UserResponse toResponse(User user);

    /**
     * Maps a user to the minimal reference embedded in other responses.
     *
     * @param user the user to map
     * @return the user reference
     */
    @Mapping(target = "image", source = "imageUrl")
    UserRef toRef(User user);

    /**
     * Flattens provider links to their types, sorted so the JSON order is stable.
     *
     * @param providers the linked identities
     * @return the provider types in name order
     */
    default List<AuthProviderType> toProviderTypes(Set<AuthProvider> providers) {
        return providers.stream()
                .map(AuthProvider::getProvider)
                .sorted(Comparator.comparing(AuthProviderType::name))
                .toList();
    }
}
