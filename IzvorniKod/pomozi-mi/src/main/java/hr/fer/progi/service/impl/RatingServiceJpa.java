package hr.fer.progi.service.impl;

import hr.fer.progi.dao.RatingRepository;
import hr.fer.progi.dao.UserRepository;
import hr.fer.progi.domain.Rating;
import hr.fer.progi.service.RatingService;
import hr.fer.progi.service.exceptions.NonexistingUserReferencedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Implementation of {@link RatingService} interface.
 */
@Service
public class RatingServiceJpa implements RatingService {

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<Rating> userRatings(String username) {
        return ratingRepository.findAllWhereUserIsRated(username);
    }

    @Override
    public List<Rating> authoredRatings(String username) {
        return ratingRepository.findAllWhereUserHasRated(username);
    }

    @Override
    public Rating addRating(Rating rating) {
        Assert.notNull(rating, "Rating object must be given");

        return ratingRepository.save(rating);
    }

    @Override
    public double calculateAverageRatingForUser(String username) {
        if (userRepository.findByUsername(username) == null)
            throw new NonexistingUserReferencedException("No User with username: '" + username + "' in database");

        List<Rating> ratingList = ratingRepository.findAllWhereUserIsRated(username);

        return ratingList.size() == 0 ? 0.0 : ratingList.stream()
                .mapToInt(r -> r.getRating())
                .average()
                .getAsDouble();
    }
}
