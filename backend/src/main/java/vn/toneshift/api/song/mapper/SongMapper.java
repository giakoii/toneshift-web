package vn.toneshift.api.song.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.toneshift.api.common.dto.UserRef;
import vn.toneshift.api.song.dto.SongResponse;
import vn.toneshift.api.song.dto.SongSummary;
import vn.toneshift.api.song.dto.SongSummaryRow;
import vn.toneshift.api.song.entity.Song;
import vn.toneshift.api.user.mapper.UserMapper;

/**
 * Maps {@link Song} entities and list rows to the API representations.
 */
@Mapper(uses = UserMapper.class, imports = UserRef.class)
public interface SongMapper {

    /**
     * Maps a song to the full response.
     *
     * @param song the song to map, with its owner reachable
     * @return the song response, with no viewer rating
     */
    @Mapping(target = "myScore", ignore = true)
    SongResponse toResponse(Song song);

    /**
     * Maps a list row to the list representation, rebuilding the nested owner reference.
     *
     * @param row the flattened query row
     * @return the song summary
     */
    @Mapping(target = "owner",
            expression = "java(new UserRef(row.ownerId(), row.ownerName(), row.ownerImage()))")
    SongSummary toSummary(SongSummaryRow row);
}
