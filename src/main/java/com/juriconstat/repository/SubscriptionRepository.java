package com.juriconstat.repository;

import com.juriconstat.model.Subscription;
import com.juriconstat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    boolean existsByFollowerAndFollowing(User follower, User following);
    void deleteByFollowerAndFollowing(User follower, User following);
    int countByFollowing(User following);
    int countByFollower(User follower);
}
