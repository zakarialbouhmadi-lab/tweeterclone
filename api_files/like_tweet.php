<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    $tweet_id = $_POST['tweet_id'];
    $user_id = $_POST['user_id'];

    // Check if user has already liked the tweet
    $check_stmt = $conn->prepare("SELECT * FROM likes WHERE tweet_id = ? AND user_id = ?");
    $check_stmt->bind_param("ii", $tweet_id, $user_id);
    $check_stmt->execute();
    $result = $check_stmt->get_result();

    if ($result->num_rows > 0) {
        // User has already liked, so unlike
        $stmt = $conn->prepare("DELETE FROM likes WHERE tweet_id = ? AND user_id = ?");
        $stmt->bind_param("ii", $tweet_id, $user_id);
        $action = "unliked";
    } else {
        // User hasn't liked, so add like
        $stmt = $conn->prepare("INSERT INTO likes (tweet_id, user_id) VALUES (?, ?)");
        $stmt->bind_param("ii", $tweet_id, $user_id);
        $action = "liked";
    }

    if ($stmt->execute()) {
        $count_stmt = $conn->prepare("SELECT COUNT(*) as count FROM likes WHERE tweet_id = ?");
        $count_stmt->bind_param("i", $tweet_id);
        $count_stmt->execute();
        $count_row = $count_stmt->get_result()->fetch_assoc();

        $response['success'] = true;
        $response['action'] = $action;
        $response['likes_count'] = $count_row['count'];
        $response['message'] = "Tweet " . $action;
        log_info('like_tweet', "Tweet {$action} - user_id:{$user_id} tweet_id:{$tweet_id} total_likes:{$count_row['count']}");
    } else {
        $response['success'] = false;
        $response['message'] = "Error processing like";
        log_error('like_tweet', "DB error - user_id:{$user_id} tweet_id:{$tweet_id}");
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
    log_warn('like_tweet', "Invalid request method: " . $_SERVER['REQUEST_METHOD']);
}

echo json_encode($response);
?>
