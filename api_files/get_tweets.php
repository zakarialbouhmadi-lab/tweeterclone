<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();
$user_id = isset($_GET['user_id']) ? $_GET['user_id'] : 0;

try {
    $query = "
    SELECT
    t.tweet_id,
    t.user_id,
    u.username,
    u.profile_pic,
    t.content,
    t.image,
    t.created_at,
    (SELECT COUNT(*) FROM likes WHERE tweet_id = t.tweet_id) as likes_count,
    (SELECT COUNT(*) FROM comments WHERE tweet_id = t.tweet_id) as comments_count,
    EXISTS(SELECT 1 FROM likes WHERE tweet_id = t.tweet_id AND user_id = ?) as is_liked
    FROM tweets t
    JOIN users u ON t.user_id = u.user_id
    WHERE t.user_id IN (
        SELECT following_id
        FROM follows
        WHERE follower_id = ? AND status = 'accepted'
    )
    OR t.user_id = ?
    OR u.is_public = 1
    ORDER BY t.created_at DESC";

    $stmt = $conn->prepare($query);
    $stmt->bind_param("iii", $user_id, $user_id, $user_id);
    $stmt->execute();
    $result = $stmt->get_result();

    $tweets = array();
    while ($row = $result->fetch_assoc()) {
        $tweets[] = array(
            'tweet_id' => $row['tweet_id'],
            'user_id' => $row['user_id'],
            'username' => $row['username'],
            'profile_pic' => $row['profile_pic'],
            'content' => $row['content'],
            'image' => $row['image'],
            'created_at' => $row['created_at'],
            'likes_count' => intval($row['likes_count']),
            'comments_count' => intval($row['comments_count']),
            'is_liked' => $row['is_liked'] == 1
        );
    }

    $response['success'] = true;
    $response['tweets'] = $tweets;

} catch (Exception $e) {
    log_error('get_tweets', "Error - user_id:{$user_id} msg:" . $e->getMessage());
    $response['success'] = false;
    $response['message'] = "Error: " . $e->getMessage();
}

echo json_encode($response);
?>
