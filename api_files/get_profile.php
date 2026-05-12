<?php
header('Content-Type: application/json');
require_once 'config.php';

error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', 'error.log');

$response = array();

try {
    $user_id = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;
    $current_user_id = isset($_GET['current_user_id']) ? (int)$_GET['current_user_id'] : 0;

    // Get user data and follow counts (only count accepted follows)
    $profile_query = "
    SELECT
    u.*,
    (SELECT COUNT(*) FROM follows WHERE following_id = u.user_id AND status = 'accepted') as followers_count,
    (SELECT COUNT(*) FROM follows WHERE follower_id = u.user_id AND status = 'accepted') as following_count,
    (SELECT status FROM follows WHERE follower_id = ? AND following_id = u.user_id LIMIT 1) as follow_status,
    (SELECT COUNT(*) FROM follows WHERE following_id = u.user_id AND status = 'pending') as pending_requests_count
    FROM users u
    WHERE u.user_id = ?
    ";

    $stmt = $conn->prepare($profile_query);
    $stmt->bind_param("ii", $current_user_id, $user_id);
    $stmt->execute();
    $profile_result = $stmt->get_result();

    if ($profile_result->num_rows === 0) {
        throw new Exception("User not found");
    }

    $profile = $profile_result->fetch_assoc();
    
    // Determine follow status
    $follow_status = $profile['follow_status'] ?? 'none';
    if ($follow_status === null) {
        $follow_status = 'none';
    }
    
    // Check if current user can see tweets
    $is_following = ($follow_status == 'accepted');
    $is_own_profile = ($current_user_id == $user_id);
    $is_public = (bool)$profile['is_public'];
    $can_see_tweets = $is_own_profile || $is_public || $is_following;

    // Get user's tweets (only if allowed)
    $tweets = array();
    if ($can_see_tweets) {
        $tweets_query = "
        SELECT
        t.*,
        u.username,
        (SELECT COUNT(*) FROM likes WHERE tweet_id = t.tweet_id) as likes_count,
        (SELECT COUNT(*) FROM comments WHERE tweet_id = t.tweet_id) as comments_count,
        EXISTS(SELECT 1 FROM likes WHERE tweet_id = t.tweet_id AND user_id = ?) as is_liked
        FROM tweets t
        JOIN users u ON t.user_id = u.user_id
        WHERE t.user_id = ?
        ORDER BY t.created_at DESC
        ";

        $stmt = $conn->prepare($tweets_query);
        $stmt->bind_param("ii", $current_user_id, $user_id);
        $stmt->execute();
        $tweets_result = $stmt->get_result();

        while ($row = $tweets_result->fetch_assoc()) {
            $tweets[] = array(
                'tweet_id' => (int)$row['tweet_id'],
                'user_id' => (int)$row['user_id'],
                'username' => $row['username'],
                'content' => $row['content'],
                'image' => $row['image'],
                'created_at' => $row['created_at'],
                'likes_count' => (int)$row['likes_count'],
                'comments_count' => (int)$row['comments_count'],
                'is_liked' => (bool)$row['is_liked']
            );
        }
    }

    $response = array(
        'success' => true,
        'profile' => array(
            'user_id' => (int)$profile['user_id'],
            'username' => $profile['username'],
            'bio' => $profile['bio'] ?? '',
            'profile_pic' => $profile['profile_pic'] ?? '',
            'is_public' => (bool)$profile['is_public'],
            'followers_count' => (int)$profile['followers_count'],
            'following_count' => (int)$profile['following_count'],
            'is_following' => $is_following, // Keep for backward compatibility
            'follow_status' => $follow_status, // 'none', 'pending', 'accepted'
            'pending_requests_count' => (int)$profile['pending_requests_count'],
            'can_see_tweets' => $can_see_tweets
        ),
        'tweets' => $tweets
    );

} catch (Exception $e) {
    log_error('get_profile', "Error - user_id:{$user_id} current_user_id:{$current_user_id} msg:" . $e->getMessage());
    $response = array(
        'success' => false,
        'message' => $e->getMessage()
    );
}

echo json_encode($response);
?>
